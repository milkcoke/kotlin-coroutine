package coroutine.dispatcher

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class CoroutineDispatcherTest {

  @Nested
  inner class ThreadDispatcher {
    @OptIn(DelicateCoroutinesApi::class)
    @DisplayName("Single thread dispatcher")
    @Test
    fun singleThreadDispatcher(): Unit = runBlocking { // coroutine#1
      val dispatcher: CoroutineDispatcher = newSingleThreadContext("SingleThread")

      launch(context = dispatcher) { // coroutine#2
        println("Running in thread: ${Thread.currentThread().name}")
      }
    }

    @OptIn(DelicateCoroutinesApi::class)
    @DisplayName("Multi thread dispatcher")
    @Test
    fun multiThreadsDispatcher(): Unit = runBlocking { // coroutine#1
      val dispatcher: CoroutineDispatcher = newFixedThreadPoolContext(
        nThreads = 2,
        name = "MultiThread"
      )

      launch(context = dispatcher) { // coroutine#2
        println("Running in thread: ${Thread.currentThread().name}")
      }

      launch(context = dispatcher) { // coroutine#3
        println("Running in thread: ${Thread.currentThread().name}")
      }
    }

    @OptIn(DelicateCoroutinesApi::class)
    @DisplayName("Children coroutines inherit parent dispatcher")
    @Test
    fun parentChildDispatcherTest(): Unit = runBlocking { // coroutine#1
      val dispatcher: CoroutineDispatcher = newFixedThreadPoolContext(
        nThreads = 2,
        name = "MultiThread"
      )

      launch(dispatcher) {
        // Parent Coroutine (#2)
        println("Parent coroutine running in thread: ${Thread.currentThread().name}")

        launch { // Child Coroutine (#3)
          println("Child coroutine running in thread: ${Thread.currentThread().name}")
        }

        launch { // Child Coroutine (#4)
          println("Another Child coroutine running in thread: ${Thread.currentThread().name}")
        }
      }
    }

  }

  @Nested
  inner class PredefinedDispatchers {
    @DisplayName("IO Dispatcher")
    @Test
    fun ioDispatcherTest(): Unit = runBlocking {
      launch(context = Dispatchers.IO) { // singleton instance
        println("IO Dispatcher running in thread: ${Thread.currentThread().name}")
      }
    }

    @DisplayName("CPU Dispatcher")
    @Test
    fun cpuDispatcherTest(): Unit = runBlocking {
      launch(context = Dispatchers.Default) { // singleton instance
        println("CPU Dispatcher running in thread: ${Thread.currentThread().name}")
      }
    }


    @DisplayName("Defaults Limited parallelism Dispatcher")
    @Test
    fun cpuLimitedParallelismTest(): Unit = runBlocking {
      launch(Dispatchers.Default.limitedParallelism(2)) {
        repeat(10) {
          launch {
            // all thread names are DefaultDispatcher-worker-*
            println("Running in thread: ${Thread.currentThread().name}")
          }
        }
      }
    }

    @DisplayName("IO limited parallelism Dispatcher")
    @Test
    fun ioLimitedParallelismTest(): Unit = runBlocking {
      launch(Dispatchers.IO.limitedParallelism(2)) {
        repeat(10) {
          launch {
            Thread.sleep(1000L)
            println("Running in thread: ${Thread.currentThread().name}")
          }
        }
      }
    }

  }

  /** 무제한 디스패처 (Dispatchers.Unconfined) 는
  / 자신을 실행시킨 스레드에서 즉시 코루틴을 실행한다.
  */
  @DisplayName("Unconfined Dispatchers")
  @Test
  fun unconfinedDispatchersTest(): Unit = runBlocking {
    println("${Thread.currentThread().name} 실행")
    launch(Dispatchers.Unconfined) {
      println("${Thread.currentThread().name} 실행")
    }
  }

  /** Undispatched 시작될때 즉시 실행 스레드에서 코루틴이 시작되는 대신
  * 일시 중단 이후 재개될 때는 다시 Dispatchers 에 코루틴 -> 쓰레드 할당 요청이 이뤄진다.
  * 따라서 자신을 처음에 실행시킨 스레드에서 실행된다는 보장이 없다.
  * */
  @DisplayName("Undispatched Dispatchers")
  @Test
  fun undispatchedDispatchersTest(): Unit = runBlocking {
    launch(start = CoroutineStart.UNDISPATCHED) {
      println("${Thread.currentThread().name} 실행 시작")
      delay(10L)
      println("${Thread.currentThread().name} 실행 완료")
    }
  }



}
