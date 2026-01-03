


## (0) 코루틴이란
경량 스레드로 작업의 단위다. \
코루틴은 중단/재개가 가능하다. 코루틴 중단시 스레드는 블로킹되지 않고 다른 코루틴을 실행할 수 있다. \
코루틴은 비동기 프로그래밍을 쉽게 만들어준다. 


`runBlocking` 함수는 함수를 호출한 스레드를 사용하여 실행되는 코루틴을 만들어낸다. \
실행 완료시까지 다른 코루틴에 의해 스레드가 점유되지 않도록 블로킹한다. 

예를들어, main 스레드에서 `runBlocking`을 호출하면 main 스레드가 코루틴이 완료될 때까지 블로킹된다. \
runBlocking 인자로 들어온 람다식을 실행하며, 람다식 내부의 모든 코드 실행 완료까지 코루틴을 종료하지 않는다.

```kotlin
fun main() = runBlocking {
  println("Hello, Coroutine!")
}
```

## (1) CoroutineDispatcher
CoroutineDispatcher 는 코루틴을 스레드 또는 스레드 풀에 보내는 역할을 한다.


#### 예제
```kotlin
  @DisplayName("Multi thread dispatcher")
  @Test
  fun multiThreadsDispatcher(): Unit = runBlocking { // coroutine#1
    val dispatcher : CoroutineDispatcher = newFixedThreadPoolContext(
      nThreads = 2,
      name = "MultiThread"
    )

    launch(context = dispatcher) { // coroutine#2
      println("Running in thread: ${Thread.currentThread().name}")
    }

    launch(context = dispatcher) { // coroutine#3
      println("Running in thread: ${Thread.currentThread().name}")
    }
  }
```

실행 결과
```txt
Running in thread: MultiThread-1 @coroutine#2
Running in thread: MultiThread-1 @coroutine#3
```

#### 미리 정의된 CoroutineDispatcher

`newFixedThreadPoolContext` 함수로 객체를 만드는 것은 비효율적일 가능성이 높다. \
특정 CoroutineDispatcher 객체에서만 사용되는 스레드풀이 생성되기 때문에, 스레드 재사용성이 떨어진다. \
예를 들어 여러 개발자가 함께 개발할 경우 특정 용도를 위해 만들어진 CoroutineDispatcher 객체가 이미 메모리상 있음에도 \
해당 객체 존재를 몰라 다시 새로운 CoroutineDispatcher 객체를 만들며 스레드 생성 비용이 들게된다.

따라서 코틀린 라이브러리는 미리 정의된 CoroutineDispatcher 객체를 제공한다. 

- Dispachers.Default : CPU 집약적인 작업에 적합하다. (예: 복잡한 계산, 데이터 처리 등)
- Dispachers.IO : 입출력 작업에 적합하다. (예: 파일 읽기/쓰기, 네트워크 통신 등)
- Dispachers.Main : 메인 스레드를 사용하기 위한 디스패처다.

Dispatchers.Default 와 Dispatchers.IO 모두 싱글톤 인스턴스다.
또 둘다 코틀린 라이브러리에서 구현한 공유된 쓰레드풀을 사용한다. \
Dispatchers.Default 로 모든 스레드를 사용하면 해당 시간동안 다른 연산이 실행되지 못한다. \
이를 방지하기 위해 코루틴 라이브러리에서 Dispatchers.Default 일부 스레드만 사용해 특정 연산을 실행할 수 있게하는 `limitedParallelism` 함수를 제공한다. 

![SharedThreadPool](assets/SharedThreadPool.png)

`Default.limitedParallelism` 은 Default 스레드풀 내의 스레드를 사용한다. \
`IO.limitedParallelism` 은 공유 스레드풀 내에서 Default, IO 쓰레드풀과 격리된 새로운 스레드 풀을 새로 생성한다.
특정 작업이 다른 작업에 영향받지 않아야할 때 사용한다.

## (2) Job
코루틴을 생성하는 `runBlocking`, `launch` 함수를 코루틴 빌더라고 부른다.
코루틴 빌더 함수는 코루틴을 추상화한 Job 객체를 생성한다.

코루틴은 중단/재개가 가능한 작업의 단위라고 했다. \ 
Job 객체를 통해 코루틴의 상태 관리를 할 수 있다.

#### join() 순차대기
```kotlin
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
```

토큰 업데이트 코루틴잡이 종료되고 나서야 네트워크 요청 코루틴이 작동한다.\
순차 처리가 필요할 때는 `join()` 메소드를 사용할 수 있다. \
`join()` 호출시 대상 코루틴의 작업이 완료될 때까지 `join()` 을 호출한 코루틴이 일시 중단된다.

즉, 위에선 `runBlocking{}` 의 coroutine#1 이 updateTokenJob 의 coroutine#2 가 완료될 때까지 일시 중단된다.

#### joinAll() 복수 순차대기
여러개의 Job 이 완료되고 나서야 실행되어야 한다면 `joinAll()` API 를 활용해보자.

```kotlin
fun waitAllJobs() = runBlocking {
  val convertImageJob1 = launch(Dispatchers.Default) {
    delay(1000L)
    println("${Thread.currentThread().name} 이미지 1 변환 완료")
  }
  val convertImageJob2 = launch(Dispatchers.Default) {
    delay(1000L)
    println("${Thread.currentThread().name} 이미지 2 변환 완료")
  }

  joinAll(convertImageJob1, convertImageJob2)
  val uploadImages = launch(Dispatchers.IO) {
    println("${Thread.currentThread().name} 이미지 1,2 업로드 완료")
  }
}
```

#### cancel() 작업 취소

```kotlin
fun cancelTest() = runBlocking {
  val startTime = System.currentTimeMillis()
  val longJob = launch(Dispatchers.IO) {
    repeat(5) { idx->
      delay(1000L)
      println("${getElapsedTime(startTime)} current index: $idx")
    }
  }
  delay(2500L) 
  longJob.cancel() // 작업 취소
}
```
실행 결과 0,1 만 출력되고 2,3,4 는 취소되어 실행되지 않는다.
```txt
[Elapsed time: 1010 ms] current index: 0
[Elapsed time: 2013 ms] current index: 1
```
#### cancelAndJoin() 
cancel() API 는 비동기적으로 동작한다. \
즉, **cancel 대상이 완전히 작업이 취소되길 기다리지 않는다.**

아래와 같은 절차로 동작한다.
1. cancel() 호출
2. cancel flag 활성화로 "취소 요청됨" 상태로 전이
3. 일시 중단 시점 또는 코루틴 실행 대기 시점에 cancel flag 확인
4. cancel flag `true` 면 취소

따라서, `cancel()` 호출 직후 다른 코루틴이 실행될때, 순서가 보장되지 않는다. \
**완벽히 대상 코루틴이 '취소됨'을 보장하고 싶을 때 `cancelAndJoin()` 을 사용한다.**


