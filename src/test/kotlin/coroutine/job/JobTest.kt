package coroutine.job

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

internal class JobTest {

  @DisplayName("Can wait using join")
  @Test
  fun joinTest() = runBlocking {
    val updateTokenJob = launch(Dispatchers.IO) {
      println("${Thread.currentThread().name} 토큰 업데이트 시작")
      delay(1000L)
      println("${Thread.currentThread().name} 토큰 업데이트 완료")
    }
    updateTokenJob.join()
    val networkCallJob = launch(Dispatchers.IO) {
      println("${Thread.currentThread().name} 네트워크 요청")
    }
  }

  @DisplayName("Can wait all jobs")
  @Test
  fun waitAllJobs() = runBlocking {
    val convertImageJob1 = launch(Dispatchers.Default) {
      println("${Thread.currentThread().name} 이미지 1 변환 시작")
      delay(1000L)
      println("${Thread.currentThread().name} 이미지 1 변환 완료")
    }
    val convertImageJob2 = launch(Dispatchers.Default) {
      println("${Thread.currentThread().name} 이미지 2 변환 시작")
      delay(1000L)
      println("${Thread.currentThread().name} 이미지 2 변환 완료")
    }

    joinAll(convertImageJob1, convertImageJob2)
    val uploadImages = launch(Dispatchers.IO) {
      println("${Thread.currentThread().name} 이미지 1,2 업로드 완료")
    }
  }

  fun getElapsedTime(startTime: Long): String {
    return "[Elapsed time: ${System.currentTimeMillis() - startTime} ms]"
  }

  @DisplayName("Lazy starting")
  @Test
  fun lazyStartTest() = runBlocking<Unit> {
    val startTime = System.currentTimeMillis()
    val lazyJob = launch(start = CoroutineStart.LAZY) {
      println("${getElapsedTime(startTime)} lazy execution")
    }

    delay(1000L)
    lazyJob.start() // start should be called when Coroutine creation as LAZY
  }

  @DisplayName("Cancel job")
  @Test
  fun cancelTest() = runBlocking {
    val startTime = System.currentTimeMillis()
    val longJob = launch(Dispatchers.IO) {
      repeat(5) { idx->
        delay(1000L)
        println("${getElapsedTime(startTime)} current index: $idx")
      }
    }
    delay(2500L)
    longJob.cancel()
  }

  @DisplayName("cancel and join")
  @Test
  fun cancelAndJoinTest() = runBlocking {
    val firstJob = launch(Dispatchers.Default) {
      delay(1000L)
      println("First Long Job Done")
    }
    firstJob.cancelAndJoin()
    val secondJob = launch(Dispatchers.IO) {
      println("Second job done")
    }
  }

}
