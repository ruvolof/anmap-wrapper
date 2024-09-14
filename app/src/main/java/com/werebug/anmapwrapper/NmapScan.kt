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
    private val mainThreadHandler: Handler,
    private val libDir: String
) : Runnable {
    private var stopped = false

    override fun run() {
        val processBuilder = ProcessBuilder(command)
        processBuilder.redirectErrorStream(true)
        try {
            val process = processBuilder.start()
            mainThreadHandler.post { mainActivityRef.get()!!.initScanView() }
            val processStdout = process.inputStream
            var exited = false
            while (!exited && !stopped) {
                val outputByteCount = processStdout.available()
                if (outputByteCount > 0) {
                    val bytes = ByteArray(outputByteCount)
                    processStdout.read(bytes)
                    mainThreadHandler.post {
                        mainActivityRef.get()?.updateOutputView(
                            String(bytes, StandardCharsets.UTF_8), false
                        )
                    }
                }
                if (stopped) {
                    process.destroy()
                }
                try {
                    process.exitValue()
                    exited = true
                } catch (ignored: IllegalThreadStateException) {
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
        } catch (e: IOException) {
            Log.e(MainActivity.LOG_TAG, e.message!!)
            mainThreadHandler.post {
                Toast.makeText(mainActivityRef.get(), e.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    fun stopScan() {
        stopped = true
    }
}