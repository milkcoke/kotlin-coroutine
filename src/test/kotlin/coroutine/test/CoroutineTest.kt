package coroutine.test

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.Duration

class CoroutineTest {

  class RepeatAdd {
    suspend fun add(repeatTime: Int): Int = withContext(Dispatchers.IO) {
      var result = 0
      repeat(repeatTime) {
        delay(10)
        result += 1
      }
      return@withContext result
    }
  }

  @DisplayName("long time test")
  @Test
  fun longTimeTest(): Unit = runBlocking {
    val repeatAdd = RepeatAdd()
    val result = repeatAdd.add(100)
    assertThat(result).isEqualTo(100)
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @DisplayName("Virtual time test")
  @Test
  fun virtualTimeTest() {
    val testCoroutineScheduler = TestCoroutineScheduler()
    val testDispatcher = StandardTestDispatcher(scheduler = testCoroutineScheduler)
    val testCoroutineScope = CoroutineScope(testDispatcher)

    var result = 0
    testCoroutineScope.launch {
      delay(10000L)
      result = 1
      delay(10000L)
      result = 2
    }

    testCoroutineScheduler.advanceTimeBy(5000L)
    assertThat(result).isEqualTo(0)
    testCoroutineScheduler.advanceTimeBy(6000L)
    assertThat(result).isEqualTo(1)
    testCoroutineScheduler.advanceTimeBy(10000L)
    assertThat(result).isEqualTo(2)
  }

  @DisplayName("advanceUtilIdle Test")
  @Test
  fun advanceUntilIdleTest() {
    val testCoroutineScheduler = TestCoroutineScheduler()
    val testDispatcher = StandardTestDispatcher(scheduler = testCoroutineScheduler)
    val testCoroutineScope = CoroutineScope(testDispatcher)

    var result = 0
    testCoroutineScope.launch {
      delay(10000L)
      result += 1
      delay(10000L)
      result += 1
    }

    testCoroutineScheduler.advanceUntilIdle()

    assertThat(result).isEqualTo(2)
  }

  @DisplayName("StandardTestDispatcher supports constructor for TestCoroutineScheduler")
  @Test
  fun standardTestDispatcherTest() {
    // Create testCoroutineScheduler internally when no provided
    val testDispatcher = StandardTestDispatcher()
    val testCoroutineScope = CoroutineScope(testDispatcher)

    var result = 0
    testCoroutineScope.launch {
      delay(10000L)
      result += 1
      delay(10000L)
      result += 1
    }

    // when
    testDispatcher.scheduler.advanceUntilIdle()
    // then
    assertThat(result).isEqualTo(2)

  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @DisplayName("TestScope function return TestScope having TestDispatcher including TestCoroutineScheduler")
  @Test
  fun testScopeTest() {
    val testCoroutineScope = TestScope()

    var result = 0

    testCoroutineScope.launch {
      delay(10000L)
      result += 1
      delay(10000L)
      result += 1
    }

    // when
    testCoroutineScope.advanceUntilIdle()

    // then
    assertThat(result).isEqualTo(2)
  }

  @DisplayName("runTest can wind to the idle state time immediately")
  @Test
  fun runTestTest() {
    // given
    var result = 0

    // when
    runTest {
      delay(10000L)
      result += 1
      delay(10000L)
      result += 1
    }

    // then
    assertThat(result).isEqualTo(2)
  }


  @DisplayName("Apply runtest straightly")
  @Test
  fun ruNTestTest2() = runTest {
    var result = 0

    delay(10000L)
    result += 1
    delay(10000L)
    result += 1

    assertThat(result).isEqualTo(2)
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @DisplayName("testScope does not wind to the descendant coroutine time")
  @Test
  fun runTestDescendantTest() = runTest {
    // given
    var result = 0

    // when
    launch {
      delay(1000L)
      result += 1
    }

    // then
    assertThat(this.currentTime).isEqualTo(0)
    assertThat(result).isEqualTo(0)
    advanceUntilIdle()
    assertThat(this.currentTime).isEqualTo(1000L)
    assertThat(result).isEqualTo(1)
  }


}
