package actor

import java.util.concurrent.atomic.AtomicBoolean


// 현재 LogActor 는 로그를 더하고 flush() 하는 책임까지 모두 가진다.
// SRP 원칙에 위배된다.
// 로그 append, flush 하는 책임을 buffer 가 갖는다.
class LogBuffer(
  val capacity: Int = 5,
  private val onFlush: (List<String>) -> Unit = {println("${it.size} + logs are flushed")}
) {
  // Actor Pattern 을 사용하기 때문에 Concurrent* 시리즈 클래스를 사용할 필요가 없다.
  private val buffer = mutableListOf<String>()

  private val flushInFlight = AtomicBoolean(false)

  fun append(log: String) {
    buffer.add(log)
    if (buffer.size >= capacity) flush()
  }

  fun flush() {
    if (buffer.isEmpty()) return
    // 이미 true 였다면 false 를 반환하여 return 된다.
    // false 였다면 -> true 로 변경하고 다음 코드로 넘어간다
    if (!flushInFlight.compareAndSet(false, true)) return

    // 다음 코드는 flush 를 하는 것이다.
    try {
      val batch = buffer.toList()
      buffer.clear()
      // onFlush Event Callback 을 실행시킨다.
      onFlush(batch)
    } finally {
      // 이러나 저러나 flush 를 시도했다면 flushInFlight 는 false 로 처리된다.
      // 그 성공 여부는 관계가 없다.
      flushInFlight.set(false)
    }
  }

  fun isFlushInFlight(): Boolean  = flushInFlight.get()

}
