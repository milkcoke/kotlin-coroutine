package coroutine.exception

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.supervisorScope
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class CoroutineExceptionTest {

  @DisplayName("Exception propagation from child to parent and cancel to the descendants")
  @Test
  fun exceptionTest() = runBlocking{
    launch(CoroutineName("Coroutine1")) {
      launch(CoroutineName("Coroutine3")) {
        throw RuntimeException("Exception")
      }
      delay(100L)
      println("${Thread.currentThread().name} 실행")
    }

    launch(CoroutineName("Coroutine2")) {
      delay(100L)
      println("${Thread.currentThread().name} 실행")
    }

    delay(400L)
  }


  @DisplayName("Limit the propagation exception by SupervisorJob")
  @Test
  fun supervisorJobTest() = runBlocking {
    val supervisorJob = SupervisorJob()
    // SupervisorJob -> Coroutine1 -> Coroutine3
    launch(CoroutineName("Coroutine1") + supervisorJob) {
      launch(CoroutineName("Coroutine3")) {
        throw RuntimeException("Exception")
      }

      delay(100L)
      println("${Thread.currentThread().name} 실행")
    }
    launch(CoroutineName("Coroutine2") + supervisorJob) {
      delay(100L)
      println("${Thread.currentThread().name} 실행")
    }

    delay(400L)
  }

  @DisplayName("Create SupervisorJob with CoroutineScope")
  @Test
  fun coroutineScopeSupervisorJobTest() = runBlocking {

    val coroutineScope = CoroutineScope(SupervisorJob())
    coroutineScope.apply {
      launch(CoroutineName("Coroutine1")) {
        launch(CoroutineName("Coroutine3")) {
          throw RuntimeException("Exception")
        }
        delay(100L)
        println("${Thread.currentThread().name} 실행")
      }

     launch(CoroutineName("Coroutine2")) {
       delay(100L)
       println("${Thread.currentThread().name} 실행")
     }
    }

    delay(400L)
  }

  @DisplayName("Misleading SupervisorJob")
  @Test
  fun misLeadingSupervisorJobTest() = runBlocking {
    // SupervisorJob 을 부모로 하는 Job 을 새로 생성한다.
    // SupervisorJob -> ParentCoroutine Job-> Coroutine1 Job -> Coroutine3 Job 이된다.
    // 따라서 Coroutine3 의 에러가 ParentCoroutine 까지 전파된다.
    launch(CoroutineName("Parent Coroutine") + SupervisorJob()) {
      launch(CoroutineName("Coroutine1")) {
        launch(CoroutineName("Coroutine3")) {
          throw RuntimeException("Exception")
        }

        delay(100L)
        println("${Thread.currentThread().name} 실행")
      }

      launch(CoroutineName("Coroutine2")) {
        delay(100L)
        println("${Thread.currentThread().name} 실행")
      }
    }
    delay(400L)
  }

  @DisplayName("supervisorScope 사용한 예외 전파 제한")
  @Test
  fun supervisorScopeTest(): Unit = runBlocking {
    supervisorScope {
      launch(CoroutineName("Coroutine1")) {
        launch(CoroutineName("Coroutine3")) {
          throw RuntimeException("Exception")
        }

        delay(100L)
        println("${Thread.currentThread().name} 실행")
      }

      launch(CoroutineName("Coroutine2")) {
        delay(100L)
        println("${Thread.currentThread().name} 실행")
      }
    }
  }



}
