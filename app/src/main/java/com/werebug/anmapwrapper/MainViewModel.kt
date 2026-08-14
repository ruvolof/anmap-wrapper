package com.werebug.anmapwrapper

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MainViewModel : ViewModel() {

  private val buffer = StringBuilder()

  private val _output = MutableLiveData("")
  val output: LiveData<String> = _output

  private val _isScanning = MutableLiveData(false)
  val isScanning: LiveData<Boolean> = _isScanning

  var currentNmapScan: NmapScan? = null

  fun appendOutput(chunk: String) {
    buffer.append(chunk)
    _output.value = buffer.toString()
  }

  fun clearOutput() {
    buffer.setLength(0)
    _output.value = ""
  }

  fun setScanning(scanning: Boolean) {
    _isScanning.value = scanning
  }
}
