package coroutine.suspend

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.supervisorScope
import org.assertj.core.api.AssertionsForInterfaceTypes.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class SuspendFunctionTest {

  @DisplayName("Suspend function can be called in the coroutine")
  @Test
  fun suspendTest(): Unit = runBlocking {
    delayAndPrint("Parent Coroutine")
    launch {
      delayAndPrint("Child Coroutine")
    }
  }

  suspend fun delayAndPrint(keyword: String) {
    delay(100L)
    println(keyword)
  }

  suspend fun searchFromDB(keyword: String): Array<String> {
    delay(100L)
    return arrayOf("[DB]${keyword} 1", "[DB]${keyword} 2")
  }
  suspend fun searchFromServer(keyword: String): Array<String> {
    delay(100L)
    return arrayOf("[Server]${keyword} 1", "[Server]${keyword} 2")
  }
  suspend fun searchByKeyword(keyword: String): Array<String> = coroutineScope {
    val dbResult = async {
      searchFromDB(keyword)
    }
    val serverResult = async {
      searchFromServer(keyword)
    }
    return@coroutineScope arrayOf(*dbResult.await(), *serverResult.await())
  }

  @DisplayName("CoroutineScope should be provided in not suspend or runBlocking function")
  @Test
  fun scopeSuspendFunctionTest(): Unit = runBlocking {
    val result = searchByKeyword("Kotlin Coroutine")
    result.forEach { println(it) }
  }

  suspend fun errorThrow() {
    delay(10L)
    throw Exception("Error")
  }
  @DisplayName("All failed when coroutine is cancelled")
  @Test
  fun coroutineExceptionTest() = runBlocking {
    searchByKeyword("Kotlin Coroutine")
    errorThrow()
  }

  suspend fun supervisorScopeSearch(keyword: String): Array<String> = supervisorScope {
    launch { errorThrow() }
    val dbResult = async { searchFromDB(keyword) }
    val serverResult = async { searchFromServer(keyword) }

    return@supervisorScope arrayOf(*dbResult.await(), *serverResult.await())
  }
  @DisplayName("supervisorScope protect from killing by child coroutine error thrown")
  @Test
  fun supervisorScopeTest(): Unit = runBlocking {
    val results = supervisorScopeSearch("Kotlin Coroutine")
    assertThat(results).hasSize(4)
      .containsExactlyInAnyOrder(
        "[DB]Kotlin Coroutine 1", "[DB]Kotlin Coroutine 2",
        "[Server]Kotlin Coroutine 1", "[Server]Kotlin Coroutine 2",
      )
  }


  suspend fun searchByKeywordPartial(keyword: String): Array<String> = supervisorScope {
    val dbDeferred = async {
      throw Exception("DB Read Exception")
      searchFromDB(keyword)
    }

    val serverDeferred = async {
      searchFromServer(keyword)
    }

    val dbResults = try {
      dbDeferred.await()
    } catch (e: Exception) {
      arrayOf()
    }
    val serverResults = try {
      serverDeferred.await()
    } catch (e: Exception) {
      arrayOf()
    }

    return@supervisorScope arrayOf(*dbResults, *serverResults)
  }
  @DisplayName("Can handle partial success by supervisorScope")
  @Test
  fun searchByKeywordPartialSuccessTest(): Unit = runBlocking {
    val results = searchByKeywordPartial("Kotlin Coroutine")
    assertThat(results).hasSize(2)
      .containsAnyOf(
        "[Server]Kotlin Coroutine 1", "[Server]Kotlin Coroutine 2"
      )
  }



}
