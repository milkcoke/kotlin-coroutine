package coroutine.test

import coroutine.follwerservice.Follower
import coroutine.follwerservice.FollowerSearcher
import coroutine.follwerservice.OfficialAccountRepository
import coroutine.follwerservice.PersonAccountRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class SNSTest {
  private val officialAccountRepository = StubOfficialAccountRepository(listOf(
    Follower.OfficialAccount("A", "CompanyA"),
    Follower.OfficialAccount("B", "CompanyB"),
    Follower.OfficialAccount("C", "CompanyC"),
  ))
  private val personAccountRepository = StubPersonAccountRepository(listOf(
    Follower.PersonAccount("A", "PersonA"),
    Follower.PersonAccount("B", "PersonB"),
    Follower.PersonAccount("C", "PersonC"),
  ))
  private lateinit var followerSearcher: FollowerSearcher

  @BeforeEach
  fun setUp() {
   followerSearcher = FollowerSearcher(
     officialAccountRepository = officialAccountRepository,
     personAccountRepository = personAccountRepository,
   )
  }

  class StubOfficialAccountRepository(
    private val users: List<Follower.OfficialAccount>
  ): OfficialAccountRepository {
    override suspend fun searchByName(name: String): List<Follower.OfficialAccount> {
      delay(500L)
      return users.filter{user -> user.name.contains(name)}
    }
  }

  class StubPersonAccountRepository(
    private val users: List<Follower.PersonAccount>
  ): PersonAccountRepository {
    override suspend fun searchByName(name: String): List<Follower.PersonAccount> {
      delay(500L)
      return users.filter { user -> user.name.contains(name) }
    }
  }

  @DisplayName("Should get all account according to name")
  @Test
  fun officialAndPersonTest() = runTest {
    val searchName = "A"

    val results = followerSearcher.searchByName(searchName)

    assertThat(results).containsExactlyInAnyOrder(
      Follower.OfficialAccount("A", "CompanyA"),
      Follower.PersonAccount("A", "PersonA"),
    )
  }

  @DisplayName("Should return empty list when no id found")
  @Test
  fun noResultTest() = runTest {
    // just 100 ms elapse even though two method takes 500ms
    // since runTest skip to the advanceInIdle time!
    val results = followerSearcher.searchByName("NotFoundName")

    assertThat(results).isEmpty()
  }



}
