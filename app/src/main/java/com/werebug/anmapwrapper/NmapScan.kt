package com.werebug.anmapwrapper

import android.os.Handler
import android.util.Log
import android.widget.Toast
import java.io.IOException
import java.lang.ref.WeakReference
import java.nio.charset.StandardCharsets

class NmapScan internal constructor(
  private val mainActivityRef: WeakReference<MainActivity>,
  private val command: List<String>,
  private val mainThreadHandler: Handler
) : Runnable {
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
      Log.e(MainActivity.LOG_TAG, e.message!!)
      mainThreadHandler.post {
        Toast.makeText(mainActivityRef.get(), e.message, Toast.LENGTH_LONG).show()
        mainActivityRef.get()?.updateOutputView("", true)
      }
      return
    }
    process = startedProcess
    mainThreadHandler.post { mainActivityRef.get()!!.initScanView() }
    val processStdout = startedProcess.inputStream
    try {
      val buffer = ByteArray(4096)
      while (true) {
        val bytesRead = processStdout.read(buffer)
        if (bytesRead <= 0) break
        mainThreadHandler.post {
          mainActivityRef.get()?.updateOutputView(
            String(buffer, 0, bytesRead, StandardCharsets.UTF_8), false
          )
        }
      }
    } catch (e: IOException) {
      if (!stopped) {
        Log.e(MainActivity.LOG_TAG, e.message!!)
        mainThreadHandler.post {
          Toast.makeText(mainActivityRef.get(), e.message, Toast.LENGTH_LONG).show()
        }
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
    if (stopped) {
      mainThreadHandler.post {
        Toast.makeText(
          mainActivityRef.get(), "Stopped.", Toast.LENGTH_SHORT
        ).show()
      }
    }
    mainThreadHandler.post {
      mainActivityRef.get()?.updateOutputView("", true)
    }
  }

  fun stopScan() {
    stopped = true
    process?.destroy()
  }
}