```mermaid
sequenceDiagram
  participant Caller as External Caller
  participant LA as LogActor
  participant MB as Mailbox
  participant AC as ActorCoroutine
  participant Buf as Buffer
  participant CB as Callbacks

  Note over Caller,CB: append
  Caller->>LA: append(log)
  LA->>LA: closing is false
  LA->>MB: trySend Append
  MB-->>LA: success or fail
  LA->>LA: pendingCount increment
  LA->>CB: onBackpressure paused=true if pending over 80pct
  LA-->>Caller: true or false

  Note over Caller,CB: requestFlush
  Caller->>LA: requestFlush()
  LA->>LA: closing is false
  LA->>MB: trySend Flush
  MB-->>LA: success or fail
  LA-->>Caller: true or false

  Note over Caller,CB: ActorCoroutine consume loop
  loop for each command in mailbox
    MB->>AC: dequeue Command
    AC->>AC: pendingCount decrement
    AC->>Buf: Append - buffer.add(log)
    AC->>Buf: Append auto-flush - batch toList and clear if buffer full
    AC->>CB: Append auto-flush - onFlush(batch)
    AC->>Buf: Flush - batch toList and clear
    AC->>CB: Flush - onFlush(batch)
    AC->>CB: onBackpressure paused=false if pending under 20pct
  end

  Note over Caller,CB: close
  Caller->>LA: close()
  LA->>LA: closing compareAndSet false to true
  LA->>MB: mailbox.close()
  LA->>AC: actorJob.join() - suspend
  Note over AC: loop drains then exits
  AC->>Buf: final batch toList and clear
  AC->>CB: onFlush(batch) final flush
  AC-->>LA: actorJob complete
  LA-->>Caller: return
```

## 주요 포인트 요약

| 흐름             | 핵심                                                      |
|----------------|---------------------------------------------------------|
| append()       | trySend 실패 시 즉시 false 반환 — non-blocking                 |
| requestFlush() | Mailbox 순서를 지키므로 앞선 Append 들이 모두 처리된 후 실행               |
| ActorCoroutine | 단 하나의 코루틴만 Mailbox 를 소비 → Buffer 동기화 불필요                |
| Backpressure   | append()에서 onset, 각 command 처리 후 relief 체크              |
| close()        | compareAndSet으로 단 한번만 닫히고, actorJob.join()으로 모든 호출자가 대기 |
| final flush    | Mailbox 가 닫힌 후 루프 탈출 → 잔여 Buffer 를 마지막으로 flush          | 
