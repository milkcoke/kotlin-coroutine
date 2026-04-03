package actor

import java.util.concurrent.atomic.AtomicBoolean

/**
 * 현재의 LogActor 는
 * onBackPressure 와 backPressure 상태와 threshold 까지 모두 관리한다.
 * Actor 는 전달받은 log 에 대한 append, flush 만 결정해야지 backpressure 를 활성화 / 비활성화 하는 책임까지 지지 않아야한다.
 */
class BackPressureController(
  mailBoxCapacity: Int,
  private val onBackpressure: (paused: Boolean) -> Unit = {
    if (it) println("BackPressure is enabled")
    else println("BackPressure is disabled")
  },
) {
  private val backPressured = AtomicBoolean(false)
  private val onsetThreshold = (mailBoxCapacity * 0.8).toInt()
  private val resumeThreshold = (mailBoxCapacity * 0.2).toInt()

  fun checkOnSet(pendingCount: Int) {
    if (pendingCount >= onsetThreshold) {
      if (backPressured.compareAndSet(false, true)) {
        onBackpressure(true)
      }
    }
  }

  fun checkRelief(pendingCount: Int) {
    if (pendingCount <= resumeThreshold) {
      if (backPressured.compareAndSet(true, false)) {
        onBackpressure(false)
      }
    }
  }


  fun isBackpressured(): Boolean = backPressured.get()
}
