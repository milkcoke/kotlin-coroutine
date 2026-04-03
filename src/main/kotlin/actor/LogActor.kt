package actor

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Actor Model + Channel Mailbox 패턴
 *
 * packager PartitionActor 에서 핵심 패턴만 추출한 Toy 구현
 *
 * 핵심 아이디어:
 *   - Channel<Command> 가 "mailbox" 역할을 한다.
 *   - 단 하나의 coroutine (actorJob) 만이 mailbox 를 소비한다.
 *     → buffer 에 대한 접근이 항상 단일 스레드 순차 처리가 보장된다.
 *     → buffer 자체는 동기화가 필요 없다 (mutableListOf 사용).
 *   - AtomicBoolean / AtomicInteger 는 "바깥 스레드가 actor 의 상태를 안전하게 읽기 위한" 관찰 창이다.
 *
 * AtomicBoolean 패턴:
 *   1. closing      – compareAndSet(false, true) 로 close 를 딱 한번만 실행한다.
 *   2. backpressured – mailbox 가 가득 찰 때 외부 생산자를 일시 정지시키는 신호.
 *   3. flushInFlight – flush callback 실행 중임을 표시; re-entrant flush 방지.
 */
class LogActor(
  scope: CoroutineScope,
  val mailboxCapacity: Int = 64,
  private val buffer: Buffer<String>,
) {
  // ── Mailbox ──────────────────────────────────────────────────────────────
  // RENDEZVOUS = 0 → 호출자가 수신자와 만날 때까지 블록 (최고 강도의 백프레셔)
  // UNLIMITED   → 절대 블록하지 않음
  // 숫자 지정   → 가득 차면 trySend 실패 → 백프레셔 신호
  private val mailbox = Channel<Command>(mailboxCapacity)
  private val backPressureController =  BackPressureController(mailboxCapacity)

  // ── AtomicBoolean 플래그들 (외부 스레드에서 안전하게 읽힘) ────────────────
  private val closing = AtomicBoolean(false)

  // ── AtomicInteger 카운터 ─────────────────────────────────────────────────
  // mailbox 에 쌓인 미처리 이벤트 수를 외부에서 추적하기 위한 카운터.
  // (Channel.size 는 신뢰성이 낮으므로 별도 atomic 사용 – 실제 PartitionActor 와 동일한 이유)
  val pendingCount = AtomicInteger(0)

  // ── Actor Job: 단 하나의 coroutine 이 mailbox 를 순차 소비 ────────────────
  private val actorJob: Job = scope.launch {
    for (command in mailbox) {            // mailbox.close() 되면 루프 종료
      pendingCount.decrementAndGet()
      when (command) {
        is Command.Append -> handleAppend(command.log)
        is Command.Flush -> handleFlush()
      }
      backPressureController.checkRelief(pendingCount.get())
    }
    buffer.flush()
  }

  // ── Public API ───────────────────────────────────────────────────────────

  /**
   * 로그를 mailbox 에 non-blocking 으로 전송한다.
   * closing 상태이거나 mailbox 가 가득 찼으면 false 반환.
   */
  fun append(log: String): Boolean {
    if (closing.get()) return false
    val result = mailbox.trySend(Command.Append(log))
    if (result.isSuccess) {
      pendingCount.incrementAndGet()
      backPressureController.checkOnSet(pendingCount.get())
      return true
    }
    return false
  }

  /**
   * mailbox 에 Flush 명령을 전송한다.
   * Flush 명령은 mailbox 순서를 지키므로 이전 Append 들이 모두 처리된 뒤 실행된다.
   */
  fun requestFlush(): Boolean {
    if (closing.get()) return false
    return mailbox.trySend(Command.Flush).isSuccess
  }

  fun isFlushInFlight(): Boolean = buffer.isFlushInFlight()
  fun isBackPressured(): Boolean = backPressureController.isBackpressured()

  /**
   * Actor 를 종료한다.
   *
   * compareAndSet(false, true) 패턴:
   *   - 최초 호출자만 실제 종료 처리를 수행한다.
   *   - 이후 호출은 false 반환하고 actorJob.join() 만 수행하여 완료를 기다린다.
   */
  suspend fun close() {
    if (closing.compareAndSet(false, true)) {
      mailbox.close()     // 신규 send 차단; 이미 쌓인 항목은 drain
    }
    actorJob.join()         // actor coroutine 종료 대기 (여러 호출자 모두 대기)
  }

  // ── Observable state (외부 스레드에서 안전하게 읽기) ──────────────────────
  fun isClosing(): Boolean = closing.get()

  // ── Actor 내부 핸들러 (단일 coroutine 에서만 호출됨) ─────────────────────

  private fun handleAppend(log: String) {
    buffer.append(log)
  }

  private fun handleFlush() {
    buffer.flush()
  }

  // ── Command (Actor 의 메시지 타입) ────────────────────────────────────────
  sealed interface Command {
    data class Append(val log: String) : Command
    data object Flush : Command
  }
}
