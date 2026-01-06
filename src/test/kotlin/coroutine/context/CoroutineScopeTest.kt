package coroutine.context

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.coroutines.CoroutineContext

class CoroutineScopeTest {
  class CustomCoroutineScope: CoroutineScope {
    override val coroutineContext: CoroutineContext = Job() + newSingleThreadContext("CustomScopeThread")
  }

  @DisplayName("CustomCoroutineScope creation")
  @Test
  fun customCoroutineScopeCreationTest() = runBlocking {
    val coroutineScope = CustomCoroutineScope()
    coroutineScope.launch {
      delay(100L)
      println("${Thread.currentThread().name} 실행 완료")
    }
    delay(500L)
  }

  @DisplayName("Default CoroutineScope creation")
  @Test
  fun coroutineScopeCreationTest() = runBlocking {
    val scope = CoroutineScope(Dispatchers.IO)
    scope.launch {
      delay(100L)
      println("${Thread.currentThread().name} 실행 완료")
    }
    delay(500L)
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @DisplayName("CoroutineScope CoroutineBuilder Test")
  @Test
  fun coroutineBuilderInheritanceTest() {
    val parentScope = CoroutineScope(CoroutineName("ParentCoroutine") + Dispatchers.IO)
    parentScope.launch(CoroutineName("ChildCoroutine")) {
      assertThat(this.coroutineContext[CoroutineName]).isEqualTo(CoroutineName("ChildCoroutine"))
      assertThat(this.coroutineContext[CoroutineDispatcher]).isEqualTo(Dispatchers.IO)
      assertThat(this.coroutineContext[Job]!!.parent).isSameAs(parentScope.coroutineContext[Job])
    }

  }

  @DisplayName("New scope out of parent CoroutineScope")
  @Test
  fun outOfScopeTest(): Unit = runBlocking {
    launch(CoroutineName("ParentCoroutine")) {
      println("${Thread.currentThread().name} 실행")
      launch(CoroutineName("ChildCoroutine")) {
        println("${Thread.currentThread().name} 실행")
      }
      CoroutineScope(Dispatchers.IO).launch(CoroutineName("IsolatedCoroutine")) {
        println("${Thread.currentThread().name} 실행")
      }
    }
  }

  @DisplayName("CoroutineScope cancellation test")
  @Test
  fun coroutineCancellationTest(): Unit = runBlocking {
    launch(CoroutineName("ParentCoroutine")) {
      launch(CoroutineName("ChildCoroutine1")) {
        delay(100L)
        println("${Thread.currentThread().name} 실행")
      }
      launch(CoroutineName("ChildCoroutine2")) {
        delay(100L)
        println("${Thread.currentThread().name} 실행")
      }
      this.cancel()
    }

    launch(CoroutineName("ParentCoroutine2")) {
      delay(100L)
      println("${Thread.currentThread().name} 실행")
    }
  }


  @DisplayName("CoroutineScope status check")
  @Test
  fun coroutineScopeStatusTest() = runBlocking {
    val whileJob : Job = launch(Dispatchers.Default) {
      while(this.isActive) {
        delay(45L)
        println("작업 중")
      }
    }
    delay(100L)
    whileJob.cancel()
  }


  @DisplayName("Out Of Job")
  @Test
  fun jobOutOfHierarchyTest(): Unit = runBlocking {
    val newRootJob = Job()
    launch(CoroutineName("ParentCoroutine") + newRootJob) {
      launch(CoroutineName("ChildCoroutine1")) {
        delay(100L)
        println("${Thread.currentThread().name} 실행")
      }
      launch(CoroutineName("ChildCoroutine2")) {
        delay(100L)
        println("${Thread.currentThread().name} 실행")
      }
    }
    launch(CoroutineName("ParentCoroutine2") + Job()) {
      launch(CoroutineName("ChildCoroutine3")) {
        delay(100L)
        println("${Thread.currentThread().name} 실행")
      }
    }
    newRootJob.cancel()
    delay(500L)
  }

  @DisplayName("Not complete automatically")
  @Test
  fun separatedJobNotCompleteTest(): Unit = runBlocking {
    launch(CoroutineName("ParentCoroutine")) {
      val parentJob = this.coroutineContext[Job]
      val newJob = Job(parent = parentJob)

      launch(CoroutineName("ChildCoroutine") + newJob) {
        delay(100L)
        println("${Thread.currentThread().name} 실행")
      }
      newJob.complete()
    }
  }







}
