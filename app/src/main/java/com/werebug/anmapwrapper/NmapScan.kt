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

  interface Listener {
    fun onChunk(chunk: String)
    fun onError(message: String)
    fun onFinished(stoppedByUser: Boolean)
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
        listener.onFinished(false)
      }
      return
    }
    process = startedProcess
    val processStdout = startedProcess.inputStream
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
        startedProcess.waitFor()
      } catch (_: InterruptedException) {
        Thread.currentThread().interrupt()
      }
    }
    val wasStopped = stopped
    mainHandler.post { listener.onFinished(wasStopped) }
  }

  fun stopScan() {
    stopped = true
    process?.destroy()
  }
}
