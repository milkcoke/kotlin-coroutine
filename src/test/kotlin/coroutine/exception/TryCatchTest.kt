package coroutine.exception

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class TryCatchTest {
  @DisplayName("Catch state handle exception")
  @Test
  fun catchTest(): Unit = runBlocking {
    launch(CoroutineName("Coroutine1")) {
      try {
        throw RuntimeException("Exception in coroutine1")
      } catch (e: Exception) {
        assertThat(e.message).isEqualTo("Exception in coroutine1")
      }
    }
  }

  @DisplayName("Async await try catch")
  @Test
  fun asyncTryCatchTest(): Unit = runBlocking {
    supervisorScope {
      val deferred = async(CoroutineName("Coroutine1")) {
        throw RuntimeException("Exception in coroutine1")
      }
      try {
        deferred.await()
      } catch (e: Exception) {
        assertThat(e.message).isEqualTo("Exception in coroutine1")
      }
    }
  }

  @DisplayName("Should catch even though await is not called")
  @Test
  fun notAwaitExceptionPropagationTest(): Unit = runBlocking {
    // should catch this since exception is to be propagated.
    val deferred = async(CoroutineName("Coroutine1")) {
      throw RuntimeException("Exception in coroutine1")
    }

    launch(CoroutineName("Coroutine2")) {
      delay(100L)
      println("Coroutine2 is executed")
    }
  }

  @DisplayName("Should apply supervisorScope for not propagation in async coroutine")
  @Test
  fun supervisorScopeTest(): Unit = runBlocking {
    supervisorScope {
      val deferred = async(CoroutineName("Coroutine1")) {
        throw RuntimeException("Exception in coroutine1")
      }

      val result = withContext(CoroutineName("Coroutine2")) {
        delay(100L)
        return@withContext 5
      }

      assertThat(result).isEqualTo(5)
    }
  }




}
