package coroutine.suspend

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.AssertionsForInterfaceTypes.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class ContinuationTest {
  @DisplayName("When resume DefaultExecutor calling resumeWith the Continuation instance")
  @Test
  fun suspendResumeTest(): Unit = runBlocking {
    launch(Dispatchers.Unconfined) {
      assertThat(Thread.currentThread().name).contains("main")
      delay(100)
      assertThat(Thread.currentThread().name).contains("DefaultExecutor")
    }
  }

}
