package coroutine.exception

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.coroutines.cancellation.CancellationException

class CancellationExceptionTest {
  @DisplayName("CancellationException does not propagate to the parent coroutine")
  @Test
  fun cancellationExceptionPropagationTest(): Unit = runBlocking {
    val coroutine1Result = withContext(CoroutineName("Coroutine1")) {
      launch(CoroutineName("Coroutine2")) {
        throw CancellationException()
      }

      delay(10L)
      return@withContext 5
    }

    assertThat(coroutine1Result).isEqualTo(5)
  }


  @DisplayName("CancellationException only propagates to the children coroutines")
  @Test
  fun childrenCoroutinePropagationTest(): Unit = runBlocking {
    launch(CoroutineName("ParentCoroutine")) {
      throw CancellationException()
      launch(CoroutineName("ChildCoroutine1")) {
        println("Coroutine1 is executed")
      }
      launch(CoroutineName("ChildCoroutine2")) {
        println("Coroutine2 is executed")
      }
    }
  }

  @DisplayName("CancellationException thrown when job is cancelled")
  @Test
  fun jobCancelTest(): Unit = runBlocking {
    val job = launch {
      delay(10L)
    }

    job.invokeOnCompletion { exception ->
      assertThat(exception).isInstanceOf(CancellationException::class.java)
    }

    job.cancel()
  }
}
