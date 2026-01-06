package coroutine.context

import coroutine.support.Timer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class RunBlockingAndLaunch {

  @DisplayName("RunBlocking should block caller thread")
  @Test
  fun runBlockingTest() = runBlocking {
    val startTime = System.currentTimeMillis()
    runBlocking {
      delay(1000L)
      println("${Timer.getElapsedTime(startTime)}, ${Thread.currentThread().name} 하위 코루틴 종료")
    }
    println("${Thread.currentThread().name} 실행 완료")
  }

  @DisplayName("launch should not block caller thread")
  @Test
  fun launchTest() = runBlocking {
    val startTime = System.currentTimeMillis()
    launch {
      delay(1000L)
      println("${Timer.getElapsedTime(startTime)}, ${Thread.currentThread().name} 하위 코루틴 종료")
    }
    println("${Thread.currentThread().name} 실행 완료")
  }


}
