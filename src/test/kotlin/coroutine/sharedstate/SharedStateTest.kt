package coroutine.sharedstate

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger

class SharedStateTest {
  @DisplayName("Can occur race condition when to use mutable variable in coroutines")
  @Test
  fun raceConditionProblem(): Unit = runBlocking {
    var count = 0
    withContext(Dispatchers.Default) {
      repeat(100) {
        launch { count += 1 }
      }
    }
    assertThat(count).isLessThan(100)
  }

  @DisplayName("Can resolve by Mutex locking")
  @Test
  fun raceConditionResolveByLock(): Unit = runBlocking {
    var count = 0
    // kotlin mutex does not block the thread, just suspend the coroutine when lock is not available
    val mutex = Mutex()
    withContext(Dispatchers.Default) {
      repeat(100) {
        launch {
          // lock and release lock after block execution
          mutex.withLock { count += 1 }
        }
      }
    }
    assertThat(count).isEqualTo(100)
  }


  @DisplayName("Can resolve race condition by AtomicInteger")
  @Test
  fun raceConditionResolveTest(): Unit = runBlocking {
    val atomicCount = AtomicInteger(0)
    withContext(Dispatchers.Default) {
      repeat(100) {
        launch { atomicCount.incrementAndGet() }
      }
    }
    assertThat(atomicCount.get()).isEqualTo(100)
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @DisplayName("Can resolve race condition by single thread")
  @Test
  fun raceConditionResolveBySingleThread() : Unit = runBlocking {
    var count = 0
    val countChangeDispatcher = Dispatchers.IO.limitedParallelism(1)

    withContext(Dispatchers.Default) {
      repeat(100) {
        launch(countChangeDispatcher) { count += 1}
      }
    }

    assertThat(count).isEqualTo(100)
  }


}
