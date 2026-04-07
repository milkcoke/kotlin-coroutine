package actor.flusher

fun interface Flusher<T> {
  fun onFlush(batch: List<T>)
}
