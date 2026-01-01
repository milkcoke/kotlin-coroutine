


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
