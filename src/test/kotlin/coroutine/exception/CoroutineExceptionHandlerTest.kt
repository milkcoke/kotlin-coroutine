package coroutine.exception

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class CoroutineExceptionHandlerTest {

  @DisplayName("CoroutineExceptionHandler supports common exception handling")
  @Test
  fun commonLoggingTest() = runBlocking {
    val exceptionHandler = CoroutineExceptionHandler { coroutineContext, exception ->
      println("Coroutine ${coroutineContext[CoroutineName]} ${exception.message}")
    }

    CoroutineScope(Dispatchers.IO)
      .launch(CoroutineName("Coroutine1") + exceptionHandler) {
        // the exception handling is done in the coroutine 1
        launch(CoroutineName("Coroutine2")) {
          throw Exception("Coroutine2 Exception")
        }
      }

    delay(50L)
  }

  @DisplayName("CoroutineExceptionHandler is not executed when coroutine propagates to the another coroutine until no more parent coroutine")
  @Test
  fun exceptionPropagationTest(): Unit = runBlocking {
    val exceptionHandler = CoroutineExceptionHandler { coroutineContext, exception ->
      println("Coroutine ${coroutineContext[CoroutineName]} ${exception.message}")
    }

    CoroutineScope(Dispatchers.IO).launch(CoroutineName("Coroutine1")) {
      launch(CoroutineName("Coroutine2") + exceptionHandler) {
        throw Exception("Coroutine2 Exception")
      }
      delay(10L)
      println("Coroutine1 executed")
    }
    delay(10L)
  }

  @DisplayName("CoroutineExceptionHandler handle in the root launched coroutine")
  @Test
  fun handleCoroutineExceptionTest() = runBlocking {
    val exceptionHandler = CoroutineExceptionHandler { coroutineContext, exception ->
      println("Coroutine ${coroutineContext[CoroutineName]} ${exception.message}")
    }

    val exceptionHandler2 = CoroutineExceptionHandler { coroutineContext, exception ->
      println("Coroutine222 ${coroutineContext[CoroutineName]} ${exception.message}")
    }

    CoroutineScope(exceptionHandler)
      .launch(CoroutineName("Coroutine1") + exceptionHandler2) {
        launch(CoroutineName("Coroutine2")) {
          throw Exception("Coroutine2 Exception")
        }
      }

    delay(50L)
  }



}
