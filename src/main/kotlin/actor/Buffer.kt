package actor

interface Buffer<T> {
  fun append(log: T)
  fun flush()
  fun isFlushInFlight(): Boolean
}
