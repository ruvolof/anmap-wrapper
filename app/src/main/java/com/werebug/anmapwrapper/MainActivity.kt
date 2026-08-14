package com.werebug.anmapwrapper

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.HandlerCompat
import androidx.preference.PreferenceManager
import com.werebug.anmapwrapper.databinding.ActivityMainBinding
import com.werebug.anmapwrapper.parser.ParserActivity
import java.io.File
import java.lang.ref.WeakReference
import java.util.Collections
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity(), View.OnClickListener {

  companion object {
    const val LOG_TAG = "ANMAPWRAPPER_CUSTOM_LOG"
    const val XML_OUTPUT_FILE = "tmp/scan_output.xml"
  }

  private val executorService = Executors.newFixedThreadPool(1)
  private val mainThreadHandler = HandlerCompat.createAsync(Looper.getMainLooper())
  private val viewModel: MainViewModel by viewModels()
  private lateinit var binding: ActivityMainBinding
  private lateinit var libDir: String
  private lateinit var nmapExecutablePath: String
  private lateinit var sharedPreferences: SharedPreferences

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    libDir = applicationInfo.nativeLibraryDir
    nmapExecutablePath = "$libDir/libnmap.so"
    sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
    binding = ActivityMainBinding.inflate(layoutInflater)
    setContentView(binding.root)
    binding.scanControlButton.setOnClickListener(this)
    binding.parseOutputButton.setOnClickListener(this)
    binding.clearOutputButton.setOnClickListener(this)
    viewModel.output.observe(this) { renderOutput() }
    viewModel.isScanning.observe(this) { scanning ->
      binding.scanControlButton.setImageResource(
        if (scanning) android.R.drawable.ic_media_pause else android.R.drawable.ic_menu_send
      )
      if (scanning || viewModel.output.value.isNullOrEmpty()) {
        hidePostScanButtons()
      } else {
        showPostScanButtons()
      }
      renderOutput()
    }
    executorService.execute(ImportNmapAssets(WeakReference(this)))
    File(filesDir, "tmp").mkdirs()
    if (savedInstanceState == null) {
      cleanTmpFiles()
    }
  }

  private fun renderOutput() {
    val text = viewModel.output.value.orEmpty()
    binding.outputTextView.text = when {
      text.isNotEmpty() -> text
      viewModel.isScanning.value == true -> ""
      else -> getString(R.string.main_credits)
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    if (isFinishing) {
      cleanTmpFiles()
    }
    executorService.shutdown()
  }

  override fun onCreateOptionsMenu(menu: Menu?): Boolean {
    menuInflater.inflate(R.menu.main_menu, menu)
    return true
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    return when (item.itemId) {
      R.id.action_settings -> {
        // Handle Settings click
        startSettingsActivity()
        true
      }

      else -> super.onOptionsItemSelected(item)
    }
  }

  private fun patchBinaryPaths(argv: MutableList<String>) {
    val isSudo = argv.indexOf("sudo")
    if (isSudo > 0) {
      throw Exception(getString(R.string.invalid_sudo_syntax))
    }
    if (isSudo == 0) {
      argv.removeAt(0)
      argv.addAll(0, listOf("su", "-c"))
    }
    val nmapIndex = argv.indexOf("nmap")
    if (isSudo == -1 && nmapIndex != 0 || isSudo == 0 && nmapIndex != 2) {
      throw Exception(getString(R.string.invalid_nmap_syntax))
    }
    argv.removeAt(nmapIndex)
    argv.add(nmapIndex, nmapExecutablePath)
  }

  private fun patchReserved(argv: MutableList<String>, flag: String, value: String) {
    if (argv.contains(flag)) {
      throw Exception(getString(R.string.reserved_nmap_flag_error, flag))
    }
    Collections.addAll(argv, flag, value)
  }

  private fun patchDefault(argv: MutableList<String>, flag: String, value: String) {
    if (!argv.contains(flag)) {
      Collections.addAll(argv, flag, value)
    }
  }

  private fun getNmapCommandArguments(): List<String> {
    val argv =
      binding.nmapCommandInput.text.toString().trim().split(Regex("\\s+")).toMutableList()
    patchBinaryPaths(argv)
    patchReserved(argv, "--datadir", filesDir.toString())
    patchDefault(argv, "--dns-servers", "8.8.8.8")
    if (isParserEnabled()) {
      patchReserved(argv, "-oX", File(filesDir, XML_OUTPUT_FILE).path)
    }
    return argv
  }

  override fun onClick(view: View) {
    when (view.id) {
      R.id.scan_control_button -> {
        if (viewModel.isScanning.value == true) {
          viewModel.currentNmapScan?.stopScan()
          return
        }
        val command = try {
          getNmapCommandArguments()
        } catch (e: Exception) {
          Toast.makeText(this, e.message, Toast.LENGTH_LONG).show()
          return
        }
        cleanTmpFiles()
        viewModel.clearOutput()
        Log.d(LOG_TAG, command.toString())
        val scan = NmapScan(WeakReference(this), command, mainThreadHandler)
        viewModel.currentNmapScan = scan
        viewModel.setScanning(true)
        executorService.execute(scan)
      }

      R.id.parse_output_button -> {
        startParserActivity()
      }

      R.id.clear_output_button -> {
        viewModel.clearOutput()
        cleanTmpFiles()
        hidePostScanButtons()
      }
    }
  }

  fun initScanView() {
    // No-op: scan-start UI state is set synchronously on click via the ViewModel.
    // Kept because NmapScan still posts this callback.
  }

  fun updateOutputView(retrievedOutput: String?, finished: Boolean) {
    if (!retrievedOutput.isNullOrEmpty()) {
      viewModel.appendOutput(retrievedOutput)
    }
    if (finished) {
      viewModel.currentNmapScan = null
      viewModel.setScanning(false)
    }
  }

  private fun hidePostScanButtons() {
    binding.parseOutputButton.visibility = View.GONE
    binding.clearOutputButton.visibility = View.GONE
  }

  private fun showPostScanButtons() {
    binding.clearOutputButton.visibility = View.VISIBLE
    if (isParserEnabled() && File(filesDir, XML_OUTPUT_FILE).exists()) {
      binding.parseOutputButton.visibility = View.VISIBLE
    }
  }

  private fun startParserActivity() {
    val intent = Intent(this, ParserActivity::class.java)
    startActivity(intent)
  }

  private fun startSettingsActivity() {
    val intent = Intent(this, SettingsActivity::class.java)
    startActivity(intent)
  }

  private fun cleanTmpFiles() {
    val scanXmlOutput = File(filesDir, XML_OUTPUT_FILE)
    if (scanXmlOutput.exists()) {
      scanXmlOutput.delete()
    }
  }

  private fun isParserEnabled(): Boolean {
    return sharedPreferences.getBoolean("enable_parser", false)
  }
}
