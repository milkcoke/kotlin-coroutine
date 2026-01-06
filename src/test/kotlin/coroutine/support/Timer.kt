package coroutine.support

class Timer {
  companion object {
    fun getElapsedTime(startTime: Long): String {
      return "[Elapsed time: ${System.currentTimeMillis() - startTime} ms]"
    }
  }
}
