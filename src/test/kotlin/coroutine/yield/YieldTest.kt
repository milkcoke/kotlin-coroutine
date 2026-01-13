package coroutine.yield

import coroutine.support.Timer
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class YieldTest {
  @DisplayName("delay function yields the thread")
  @Test
  fun delayTest() = runBlocking {
    val startTime = System.currentTimeMillis()
    repeat(5) { idx ->
      launch {
        delay(100)
        println("${Timer.getElapsedTime(startTime)} 코루틴 $idx 실행 완료")
      }
    }
  }

  @DisplayName("Thread.sleep does not yield the thread")
  @Test
  fun sleepTest() {
    val startTime = System.currentTimeMillis()
    repeat(5) { idx ->
      Thread.sleep(100)
      println("${Timer.getElapsedTime(startTime)} 코루틴 $idx 실행 완료")
    }
  }

  @DisplayName("Join yield the thread")
  @Test
  fun joinTest() = runBlocking {
    val job = launch {
      println("1. launch coroutine starts")
      delay(100L)
      println("2. launch coroutine finished")
    }
    println("3. runBlocking waits the launch coroutine")
    job.join()
    println("4. runBlocking resumes after launch coroutine")
  }

  @DisplayName("yield to the runBlocking Coroutine")
  @Test
  fun yieldTest() = runBlocking {
    val job = launch {
      var count = 0
      while (this.isActive) {
        count++
        println("job is running $count")
        if (count % 100 == 0) {
          yield()
        }
      }
    }
    delay(10L)
    job.cancel()

  }

  @DisplayName("Coroutine thread can be switched after yielded")
  @Test
  fun coroutineThreadSwitchTest(): Unit = runBlocking {
    val dispatcher = newFixedThreadPoolContext(2, "Thread")
    launch(dispatcher) {
      repeat(5) {
        println("${Thread.currentThread().name} - 일시 중단")
        delay(10L)
        println("${Thread.currentThread().name} - 재개")
      }
    }

  }



}
