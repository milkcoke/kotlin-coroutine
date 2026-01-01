package coroutine.dispatcher

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
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




}
