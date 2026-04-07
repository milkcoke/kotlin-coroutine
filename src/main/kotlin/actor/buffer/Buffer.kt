package actor.buffer

interface Buffer<T> {
  fun append(log: T)
  fun flush()
  fun isFlushInFlight(): Boolean
}
