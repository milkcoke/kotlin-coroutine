package actor.flusher

class PrintFlusher: Flusher<String> {
  override fun onFlush(batch: List<String>) {
    println("${batch.size}  is flushed")
  }
}
