package coroutine.follwerservice

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class FollowerSearcher (
  private val officialAccountRepository: OfficialAccountRepository,
  private val personAccountRepository: PersonAccountRepository
) {
  suspend fun searchByName(name: String): List<Follower> = coroutineScope {
    val officialDeferred = async {
      officialAccountRepository.searchByName(name)
    }
    val personDeferred = async {
      personAccountRepository.searchByName(name)
    }

    return@coroutineScope awaitAll(officialDeferred, personDeferred).flatten()
  }
}
