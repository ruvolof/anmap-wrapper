package com.werebug.anmapwrapper

import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.core.os.HandlerCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import java.util.concurrent.Executors

class MainViewModel(app: Application) : AndroidViewModel(app), NmapScan.Listener {

  companion object {
    private const val IMPORT_PREFS_FILE = "MainActivity"
  }

  private val executor = Executors.newSingleThreadExecutor()
  private val mainHandler: Handler = HandlerCompat.createAsync(Looper.getMainLooper())
  private val buffer = StringBuilder()

  private val _output = MutableLiveData("")
  val output: LiveData<String> = _output

  private val _isScanning = MutableLiveData(false)
  val isScanning: LiveData<Boolean> = _isScanning

  private val _toastEvent = SingleLiveEvent<String>()
  val toastEvent: LiveData<String> = _toastEvent

  private var currentScan: NmapScan? = null

  init {
    val prefs = app.getSharedPreferences(IMPORT_PREFS_FILE, Context.MODE_PRIVATE)
    executor.execute(ImportNmapAssets(app.assets, app.filesDir, prefs))
  }

  fun startScan(command: List<String>) {
    if (_isScanning.value == true) return
    buffer.setLength(0)
    _output.value = ""
    _isScanning.value = true
    val scan = NmapScan(command, mainHandler, this)
    currentScan = scan
    executor.execute(scan)
  }

  fun stopScan() {
    currentScan?.stopScan()
  }

  fun clearOutput() {
    buffer.setLength(0)
    _output.value = ""
  }

  override fun onChunk(chunk: String) {
    buffer.append(chunk)
    _output.value = buffer.toString()
  }

  override fun onError(message: String) {
    _toastEvent.value = message
  }

  override fun onFinished(stoppedByUser: Boolean) {
    currentScan = null
    _isScanning.value = false
    if (stoppedByUser) {
      _toastEvent.value = getApplication<Application>().getString(R.string.scan_stopped_toast)
    }
  }

  override fun onCleared() {
    super.onCleared()
    executor.shutdown()
  }
}
