package coroutine.options

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class CoroutineStartOptionTest {
  @DisplayName("Default mode cancelled immediately in the New state")
  @Test
  fun newStateCancelTest(): Unit = runBlocking {
    val job = launch(start = CoroutineStart.DEFAULT) {
      println("launch coroutine")
    }
    assertThat(job.isActive).isTrue()
    job.cancel() // job is canceled before entering active state
    println("runBlocking coroutine")
  }

  @DisplayName("Lazy mode not start before start() is called")
  @Test
  fun lazyOptionTest(): Unit = runBlocking {
    val job = launch(start = CoroutineStart.LAZY) {
      println("launch coroutine")
    }
    assertThat(job.isActive).isFalse
    job.cancel()
  }


  @DisplayName("Not canceled immediately in the New state when to start ATOMIC")
  @Test
  fun atomicNotCancelTest(): Unit = runBlocking {
    val job = launch(start = CoroutineStart.ATOMIC) {
      println("launch coroutine")
    }
    job.cancel()
    println("runBlocking coroutine")
  }

  @DisplayName("Undispatched option run immediately in the current thread")
  @Test
  fun undispatchedStartTest() = runBlocking {
    launch(start = CoroutineStart.UNDISPATCHED) {
      println("launch coroutine")
    }
    println("runBlocking coroutine")
  }
}
