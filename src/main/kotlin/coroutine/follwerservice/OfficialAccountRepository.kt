package coroutine.follwerservice

interface OfficialAccountRepository {
  suspend fun searchByName(name: String): List<Follower.OfficialAccount>
}
