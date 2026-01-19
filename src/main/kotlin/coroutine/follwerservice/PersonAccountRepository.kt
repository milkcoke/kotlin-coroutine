package coroutine.follwerservice

interface PersonAccountRepository {
  suspend fun searchByName(name: String): List<Follower.PersonAccount>
}

