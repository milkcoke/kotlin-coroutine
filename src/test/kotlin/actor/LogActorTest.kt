package actor

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.time.delay
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

/**
 * Actor Model + Mailbox 패턴 + AtomicBoolean 스레드 안전 패턴 학습 테스트
 *
 * 학습 포인트:
 *   1. Channel 기반 mailbox → 단일 coroutine 이 순차적으로 처리 → buffer 에 lock 불필요
 *   2. AtomicBoolean.compareAndSet → 한 번만 실행되어야 하는 상태 전이를 lock-free 로 구현
 *   3. 백프레셔 신호: mailbox 가 가득 찰 때 생산자를 멈추고, 줄어들면 재개
 */
@DisplayName("Actor Model + Mailbox 패턴")
class LogActorTest {

    // ─────────────────────────────────────────────────────────────────────────
    // 1. Mailbox 기반 단일 스레드 순차 처리
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Mailbox 기반 순차 처리")
    inner class MailboxSequentialProcessing {

        @Test
        @DisplayName("여러 coroutine 이 동시에 append 해도 actor 는 순차적으로 처리한다")
        fun `multiple coroutines append concurrently but actor processes sequentially`(): Unit = runBlocking {
          // given
          val flushedLogs = CopyOnWriteArrayList<String>()
          val actor = LogActor(
            scope = this,
            mailboxCapacity = 200,
            buffer = LogBuffer(capacity = 5) {flushedLogs.addAll(it)},
          )

          // when - 10 개의 coroutine 이 동시에 append
          val jobs = (1..10).map { i ->
            launch { actor.append("log-$i") }
          }
          jobs.joinAll()
          actor.close()

          // then - 순서는 보장되지 않지만 하나도 빠짐없이 처리됨
          // (buffer 에는 lock 없이 mutableListOf 를 사용했지만 단일 coroutine 만 접근하므로 안전)
          assertThat(flushedLogs).hasSize(10)
          assertThat(flushedLogs).containsExactlyInAnyOrder(
            *(1..10).map { "log-$it" }.toTypedArray()
          )
        }

        @Test
        @DisplayName("buffer 가 bufferCapacity 에 도달하면 자동 flush 된다")
        fun `buffer auto-flushes when bufferCapacity is reached`(): Unit = runBlocking {
          // given
          val flushedBatches = CopyOnWriteArrayList<List<String>>()
          val actor = LogActor(
            scope = this,
            mailboxCapacity = 100,
            buffer = LogBuffer(3) {flushedBatches.add(it)},
          )

          // when - 딱 3개 (= bufferCapacity) append
          actor.append("log-1")
          actor.append("log-2")
          actor.append("log-3")
          actor.close()

          // then - 3개가 한 batch 로 flush 됨
          assertThat(flushedBatches).hasSize(1)
          assertThat(flushedBatches[0]).containsExactly("log-1", "log-2", "log-3")
        }

        @Test
        @DisplayName("requestFlush 는 mailbox 순서를 지키며 강제 flush 한다")
        fun `requestFlush flushes in mailbox order`(): Unit = runBlocking {
          // given
          val flushedBatches = CopyOnWriteArrayList<List<String>>()
          val actor = LogActor(
            scope = this,
            mailboxCapacity = 100,
            buffer = LogBuffer(100) {flushedBatches.add(it)},
          )

          // when
          //  mailbox 에 쌓이는 순서: Append("log-1"), Append("log-2"), Flush, Append("log-3")
          actor.append("log-1")
          actor.append("log-2")
          actor.requestFlush()        // ← Flush 명령이 앞선 두 Append 보다 뒤에 처리됨
          actor.append("log-3")
          actor.close()

          // then
          //  첫 번째 flush: ["log-1", "log-2"]  (Flush 명령 처리 시점)
          //  두 번째 flush: ["log-3"]            (close 시 최종 flush)
          assertThat(flushedBatches).hasSize(2)
          assertThat(flushedBatches[0]).containsExactly("log-1", "log-2")
          assertThat(flushedBatches[1]).containsExactly("log-3")
        }

        @Test
        @DisplayName("close 후에는 mailbox 에 쌓인 항목을 모두 drain 한 뒤 종료된다")
        fun `close drains remaining mailbox items before shutdown`(): Unit = runBlocking {
          // given
          val flushedLogs = CopyOnWriteArrayList<String>()
          val actor = LogActor(
            scope = this,
            mailboxCapacity = 100,
            buffer = LogBuffer(100) {flushedLogs.addAll(it)},
          )

          // when - close 전에 쌓아둔 항목들
          repeat(5) { actor.append("log-$it") }
          actor.close()   // drain 보장

          // then - close 이전에 append 된 항목은 모두 처리됨
          assertThat(flushedLogs).hasSize(5)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. AtomicBoolean 스레드 안전 패턴
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("AtomicBoolean 스레드 안전 패턴")
    inner class AtomicBooleanPattern {

        /**
         * compareAndSet(false, true) 패턴:
         *   - 여러 coroutine 이 동시에 close 를 호출해도 내부 종료 로직은 딱 한번만 실행된다.
         *   - CAS 에 실패한 호출자는 actorJob.join() 만 수행한다.
         *
         * nxlog-packager PartitionActor:
         *   if (closing.compareAndSet(false, true)) { ... }
         */
        @Test
        @DisplayName("closing compareAndSet: 여러 번 close 를 호출해도 종료 로직은 한번만 실행된다")
        fun `closing compareAndSet ensures shutdown runs exactly once`(): Unit = runBlocking {
          // given
          val flushCount = AtomicInteger(0)
          val actor = LogActor(
            scope = this,
            buffer = LogBuffer(100) { flushCount.incrementAndGet()},
          )
          actor.append("log-1")

          // when - 5개의 coroutine 이 동시에 close
          val closeJobs = (1..5).map { launch { actor.close() } }
          closeJobs.joinAll()

          // then - flush 는 정확히 1번 (종료 시 최종 flush)
          assertThat(flushCount.get()).isEqualTo(1)
          // 모든 close() 호출이 actorJob.join() 을 통해 정상 완료됨
          assertThat(actor.pendingCount.get()).isEqualTo(0)
        }

        /**
         * closing 플래그 = true 이면 append 는 즉시 false 반환하고 mailbox 에 넣지 않는다.
         *
         * nxlog-packager PartitionActor:
         *   if (closing.get()) return  // enqueue 진입 전에 조기 반환
         */
        @Test
        @DisplayName("closing 플래그: close 이후 append 는 mailbox 에 들어가지 않는다")
        fun `closing flag rejects appends after close is initiated`(): Unit = runBlocking {
          // given
          val flushedLogs = CopyOnWriteArrayList<String>()
          val actor = LogActor(
            scope = this,
            buffer = LogBuffer(100) { batch -> flushedLogs.addAll(batch) },
          )
          actor.append("before-close")
          actor.close()

          // when - close 이후 append
          val accepted = actor.append("after-close")

          // then
          assertThat(accepted).isFalse()
          assertThat(flushedLogs).containsExactly("before-close")
          assertThat(flushedLogs).doesNotContain("after-close")
        }

        /**
         * 백프레셔 onset/relief:
         *   - mailbox 가 80% 이상 차면 compareAndSet(false, true) 로 onBackpressure(true) 를 한 번 호출
         *   - mailbox 가 20% 이하로 줄면 compareAndSet(true, false) 로 onBackpressure(false) 를 한 번 호출
         *
         * nxlog-packager PartitionActor:
         *   if (backpressured.compareAndSet(false, true)) onPauseRequested(partition)
         *   if (backpressured.compareAndSet(true, false)) onResumeRequested(partition)
         */
        @Test
        @DisplayName("backpressured compareAndSet: mailbox 가 80% 이상 차면 정확히 한 번 pause 신호를 보낸다")
        fun `backpressured compareAndSet fires pause signal exactly once at onset threshold`() = runBlocking {
          // given
          val actor = LogActor(
            scope = this,
            mailboxCapacity = 10,
            buffer = LogBuffer(100),
          )

          // when - 8개 append → threshold(10 * 0.8 = 8) 도달
          repeat(8) { i -> actor.append("log-$i") }

          // then
          assertThat(actor.isBackPressured()).isTrue()
          actor.close()
        }

        @Test
        @DisplayName("backpressured compareAndSet: mailbox 가 20% 이하로 줄면 resume 신호를 한 번 보낸다")
        fun `backpressured compareAndSet fires resume signal exactly once at relief threshold`() = runBlocking {
          // given
          val actor = LogActor(
            scope = this,
            mailboxCapacity = 10,
            buffer = LogBuffer(100),
          )

          // when 1) 백프레셔 onset
          repeat(8) { i -> actor.append("log-$i") }
          assertThat(actor.isBackPressured()).isTrue()

          // when 2) actor 가 처리하여 pendingCount 감소 → relief
          // actor coroutine 에게 처리할 시간을 준다
          delay(Duration.ofMillis(100))

          // then
          assertThat(actor.isBackPressured()).isFalse()

          actor.close()
        }

        /**
         * flushInFlight compareAndSet:
         *   - flush callback 실행 중에는 flushInFlight = true
         *   - finally 블록에서 반드시 false 로 복원 (lock 해제와 동일한 효과)
         *   - 단일 coroutine actor 이므로 실제 동시 flush 는 없지만
         *     외부 관찰자(폴링 스레드 등)가 "flush 진행 중" 여부를 안전하게 읽을 수 있다
         *
         * nxlog-packager PartitionActor:
         *   if (flushInFlight.compareAndSet(false, true)) { ... } finally { flushInFlight.set(false) }
         */
        @Test
        @DisplayName("flushInFlight compareAndSet: flush callback 실행 중에는 true, 완료 후에는 false")
        fun `flushInFlight is true during flush callback and false after`(): Unit = runBlocking {
          // given
          var flushInFlightDuringCallback = false
          var actorRef: LogActor? = null

          val actor = LogActor(
            scope = this,
            mailboxCapacity = 100,
            buffer = LogBuffer(1) { flushInFlightDuringCallback = actorRef!!.isFlushInFlight() }
          )
          actorRef = actor

          // when
          actor.append("log-1")       // bufferCapacity=1 이므로 즉시 handleFlush 실행
          actor.close()

          // then - callback 실행 중에 flushInFlight 는 true 였다
          assertThat(flushInFlightDuringCallback).isTrue()
          // close 후에는 false 로 복원됨
          assertThat(actor.isFlushInFlight()).isFalse()
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. Actor 없이 단순 공유 상태를 수정하면 경쟁 조건이 발생한다 (비교용)
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Actor 패턴의 필요성 (공유 상태 vs 단일 소비자)")
    inner class WhyActorMatters {

        @Test
        @DisplayName("동기화 없는 공유 리스트는 경쟁 조건으로 데이터를 잃는다")
        fun `unsynchronized shared list loses data under concurrent access`(): Unit = runBlocking {
          // given - 동기화 없는 일반 MutableList
          val unsafeList = mutableListOf<String>()

          // when - 10개 coroutine 이 동시에 add (Dispatchers.Default 에서 실행)
          val jobs = (1..10).map { i ->
            launch(Dispatchers.Default) {
              repeat(100) { unsafeList.add("log-$i-$it") }
            }
          }
          jobs.joinAll()

          assertThat(unsafeList).hasSizeLessThan(1000)
        }

        @Test
        @DisplayName("Actor(Channel mailbox) 를 쓰면 동기화 없이도 데이터 유실이 없다")
        fun `actor with channel mailbox guarantees no data loss without explicit synchronization`(): Unit =
          runBlocking {
            // given
            val safeList = mutableListOf<String>()  // actor 내부에서만 접근 → lock 불필요
            val actor = LogActor(
              scope = this,
              mailboxCapacity = 10_000,
              buffer = LogBuffer(10_000) {batch -> safeList.addAll(batch)}
            )

            // when - 10개 coroutine 이 동시에 append
            val jobs = (1..10).map { i ->
              launch(Dispatchers.Default) {
                repeat(100) { actor.append("log-$i-$it") }
              }
            }
            jobs.joinAll()
            actor.close()

            // then - 정확히 1000개 (데이터 유실 없음)
            assertThat(safeList).hasSize(1000)
          }
    }
}
