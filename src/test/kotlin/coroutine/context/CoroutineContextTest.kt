package coroutine.context

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.newSingleThreadContext
import org.assertj.core.api.AssertionsForInterfaceTypes.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.coroutines.CoroutineContext

class CoroutineContextTest {

  @OptIn(DelicateCoroutinesApi::class, ExperimentalCoroutinesApi::class)
  @DisplayName("CoroutineContext has 4 elements")
  @Test
  fun elementsTest() {
    // given
    val coroutineContext: CoroutineContext = newSingleThreadContext("MyThread") + CoroutineName("MyCoroutine")
    println(coroutineContext)
  }

  @DisplayName("Later context overwrite previous one")
  @Test
  fun overwriteTest() {
    // given
    val coroutineContext = CoroutineName("MyCoroutine")
    val newCoroutineContext = coroutineContext + CoroutineName("NewCoroutine")
    // when then
    assertThat(coroutineContext[CoroutineName]).isEqualTo(CoroutineName("MyCoroutine"))
    assertThat(newCoroutineContext[CoroutineName]).isEqualTo(CoroutineName("NewCoroutine"))
  }


  @DisplayName("Merge coroutine contexts")
  @Test
  fun mergeCoroutineContext() {
    // given
    val cotext1 = CoroutineName("Coroutine1") + newSingleThreadContext("MyThread") + Job()
    val context2 = cotext1 + CoroutineName("Coroutine2")
    // when then
    assertThat(context2[CoroutineName]).isEqualTo(CoroutineName("Coroutine2"))
    assertThat(context2[CoroutineDispatcher]).isNotNull
    assertThat(context2[Job]).isNotNull
  }

  @DisplayName("Access using singleton Key instance")
  @Test
  fun accessPropertyKeyInstanceTest() {
    // given
    val coroutineContext: CoroutineContext = Dispatchers.IO + CoroutineName("MyCoroutine")
    // when
    val coroutineName = coroutineContext[CoroutineName.Key]
    // then
    assertThat(coroutineName).isEqualTo(CoroutineName("MyCoroutine"))
  }

  @DisplayName("Access using key property")
  @Test
  fun accessPropertyTest() {
    // given
    val coroutineName = CoroutineName("MyCoroutine")
    val dispatcher = Dispatchers.IO
    val context = coroutineName + dispatcher
    // when
    assertThat(context[coroutineName.key]).isEqualTo(CoroutineName("MyCoroutine"))
    assertThat(context[dispatcher.key]).isEqualTo(Dispatchers.IO)
  }

  @DisplayName("key and Key refer to the same instance")
  @Test
  fun referenceElementSameInstanceTest() {
    // given
    val coroutineName = CoroutineName("MyCoroutine")
    // when then
    assertThat(coroutineName.key).isSameAs(CoroutineName.Key)
  }

  @DisplayName("Remove element by minusKey")
  @Test
  fun minusKeyTest() {
    // given
    val coroutineName = CoroutineName("MyCoroutine")
    val dispatcher = Dispatchers.IO
    val myJob = Job()
    val context = coroutineName + dispatcher + myJob
    // when
    val newContext = context.minusKey(Job)
    // then
    assertThat(newContext[CoroutineName]).isEqualTo(CoroutineName("MyCoroutine"))
    assertThat(newContext[CoroutineDispatcher]).isEqualTo(Dispatchers.IO)
    assertThat(newContext[Job]).isNull()
  }

  @DisplayName("minusKey does not change original instance")
  @Test
  fun minusKeyReturnAnotherInstance() {
    val coroutineName = CoroutineName("MyCoroutine")
    val dispatcher = Dispatchers.IO
    val myJob = Job()
    val context = coroutineName + dispatcher + myJob
    // when
    val newContext = context.minusKey(Job)

    // then
    assertThat(context).isNotSameAs(newContext)
    assertThat(context[Job]).isSameAs(myJob)
  }




}
