package com.werebug.anmapwrapper

import android.os.Handler
import android.util.Log
import java.io.IOException
import java.nio.charset.StandardCharsets

class NmapScan(
  private val command: List<String>,
  private val mainHandler: Handler,
  private val listener: Listener
) : Runnable {

  enum class Outcome { COMPLETED, STOPPED, FAILED }

  interface Listener {
    fun onChunk(chunk: String)
    fun onError(message: String)
    fun onFinished(outcome: Outcome)
  }

  @Volatile
  private var stopped = false

  @Volatile
  private var process: Process? = null

  override fun run() {
    val processBuilder = ProcessBuilder(command)
    processBuilder.redirectErrorStream(true)
    val startedProcess: Process
    try {
      startedProcess = processBuilder.start()
    } catch (e: IOException) {
      val message = e.message ?: e.toString()
      Log.e(MainActivity.LOG_TAG, message)
      mainHandler.post {
        listener.onError(message)
        listener.onFinished(Outcome.FAILED)
      }
      return
    }
    process = startedProcess
    val processStdout = startedProcess.inputStream
    var readFailed = false
    var exitValue = -1
    try {
      val buffer = ByteArray(4096)
      while (true) {
        val bytesRead = processStdout.read(buffer)
        if (bytesRead <= 0) break
        val chunk = String(buffer, 0, bytesRead, StandardCharsets.UTF_8)
        mainHandler.post { listener.onChunk(chunk) }
      }
    } catch (e: IOException) {
      if (!stopped) {
        readFailed = true
        val message = e.message ?: e.toString()
        Log.e(MainActivity.LOG_TAG, message)
        mainHandler.post { listener.onError(message) }
      }
    } finally {
      try {
        processStdout.close()
      } catch (_: IOException) {
      }
      try {
        exitValue = startedProcess.waitFor()
      } catch (_: InterruptedException) {
        Thread.currentThread().interrupt()
      }
    }
    val outcome = when {
      stopped -> Outcome.STOPPED
      readFailed || exitValue != 0 -> Outcome.FAILED
      else -> Outcome.COMPLETED
    }
    mainHandler.post { listener.onFinished(outcome) }
  }

  fun stopScan() {
    stopped = true
    process?.destroy()
  }
}
