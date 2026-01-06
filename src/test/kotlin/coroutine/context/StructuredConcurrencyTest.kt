package coroutine.context

import coroutine.support.Timer
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Java6Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class StructuredConcurrencyTest {

  @OptIn(DelicateCoroutinesApi::class, ExperimentalCoroutinesApi::class)
  @DisplayName("ParentChild have same CoroutineContext")
  @Test
  fun parentChildSameContext(): Unit = runBlocking {
    val coroutineContext = newSingleThreadContext("MyThread") + CoroutineName("MyCoroutine")
    launch(coroutineContext) {
      assertThat(Thread.currentThread().name).contains("MyThread")
      launch {
        assertThat(Thread.currentThread().name).contains("MyThread")
      }
    }
  }

  @OptIn(DelicateCoroutinesApi::class, ExperimentalCoroutinesApi::class)
  @DisplayName("Overwrite element in CoroutineContext")
  @Test
  fun coroutineContextExtensionTest(): Unit = runBlocking {
    val parentContext = newSingleThreadContext("MyThread") + CoroutineName("ParentCoroutine")
    launch(parentContext) {
      assertThat(Thread.currentThread().name).contains("ParentCoroutine")
      launch(CoroutineName("ChildCoroutine")) {
        assertThat(Thread.currentThread().name).contains("ChildCoroutine")
      }
    }
  }

  @DisplayName("Job is not inherited when CoroutineContext is extended")
  @Test
  fun jobInheritanceTest(): Unit = runBlocking {
    val runBlockingJob = coroutineContext[Job]
    launch {
      val launchJob = coroutineContext[Job]
      assertThat(launchJob).isNotEqualTo(runBlockingJob)
    }
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @DisplayName("Parent Child properties in the Job")
  @Test
  fun parentChildPropertiesTest(): Unit = runBlocking{
    // given
    val parentJob = coroutineContext[Job]
    launch {
      val childJob = coroutineContext[Job]
      assertThat(childJob).isNotEqualTo(parentJob)
      assertThat(childJob!!.parent).isSameAs(parentJob)
      assertThat(parentJob!!.children.contains(childJob)).isTrue
    }
  }

  @Test
  fun awaitAllTest() = runBlocking {
    val parentJob = launch(Dispatchers.IO) {
      val dbAsyncRequest = listOf("db1", "db2", "db3")
        .map {
          async {
            delay(1000)
            return@async "$it data"
          }
        }
      val dbResponse = dbAsyncRequest.awaitAll()

      assertThat(dbResponse).containsExactly("db1 data", "db2 data", "db3 data")
    }
  }

  @DisplayName("Cancel propagation from parent to children")
  @Test
  fun cancelPropagationTest() = runBlocking {
    val parentJob = launch(Dispatchers.IO) {
      val dbAsyncRequest = listOf("db1", "db2", "db3")
        .map {
          async {
            delay(1000)
            return@async "$it data"
          }
        }
      val dbResponse = dbAsyncRequest.awaitAll()
      println(dbResponse)
    }
    parentJob.cancel()
  }

  @DisplayName("Parent coroutine waits for children's completion")
  @Test
  fun parentWaitsChildrenCompletion(): Unit = runBlocking {
    val startTime = System.currentTimeMillis()
    val parentJob = launch {
      launch {
        delay(1000)
        println("${Timer.getElapsedTime(startTime)} Child coroutine completed")
      }

      println("${Timer.getElapsedTime(startTime)} Parent done")
    }

    parentJob.invokeOnCompletion {
      println("${Timer.getElapsedTime(startTime)} Parent coroutine completed")
    }
  }











































}
