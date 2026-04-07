package actor.flusher

interface Flusher<T> {
  fun onFlush(batch: List<T>)
}
