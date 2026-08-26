package com.werebug.anmapwrapper

import android.content.Intent
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.werebug.anmapwrapper.databinding.ActivityMainBinding
import com.werebug.anmapwrapper.parser.ParserActivity
import java.io.File

class MainActivity : AppCompatActivity(), View.OnClickListener {

  companion object {
    const val LOG_TAG = "ANMAPWRAPPER_CUSTOM_LOG"
    const val XML_OUTPUT_FILE = "tmp/scan_output.xml"
  }

  private val viewModel: MainViewModel by viewModels()
  private lateinit var binding: ActivityMainBinding
  private lateinit var libDir: String
  private lateinit var nmapExecutablePath: String
  private lateinit var sharedPreferences: SharedPreferences
  private var displayedLen = 0

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
    displayedLen = viewModel.outputSnapshot().length
    syncOutputView()
    viewModel.outputLen.observe(this) { len ->
      when {
        len < displayedLen -> {
          displayedLen = 0
          syncOutputView()
        }

        len > displayedLen -> {
          binding.outputTextView.editableText.append(viewModel.outputTail(displayedLen))
          displayedLen = len
        }
      }
    }
    viewModel.isScanning.observe(this) { scanning ->
      binding.scanControlButton.setImageResource(
        if (scanning) android.R.drawable.ic_media_pause else android.R.drawable.ic_menu_send
      )
      if (scanning || displayedLen == 0) {
        hidePostScanButtons()
      } else {
        showPostScanButtons()
      }
      syncOutputView()
    }
    viewModel.toastEvent.observe(this) { message ->
      Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
    File(filesDir, "tmp").mkdirs()
    if (savedInstanceState == null) {
      cleanTmpFiles()
    }
  }

  private fun syncOutputView() {
    binding.outputTextView.setText(
      when {
        displayedLen > 0 -> viewModel.outputSnapshot()
        viewModel.isScanning.value == true -> ""
        else -> getString(R.string.main_credits)
      },
      TextView.BufferType.EDITABLE
    )
  }

  override fun onDestroy() {
    super.onDestroy()
    if (isFinishing) {
      cleanTmpFiles()
    }
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

  private fun buildCommand(dnsServers: List<String>): NmapCommandBuilder.Result {
    val builder = NmapCommandBuilder(
      nmapExecutablePath = nmapExecutablePath,
      dataDirPath = filesDir.toString(),
      xmlOutputPath = if (isParserEnabled()) File(filesDir, XML_OUTPUT_FILE).path else null,
      defaultDnsServers = dnsServers,
    )
    return builder.build(binding.nmapCommandInput.text.toString())
  }

  private fun activeNetworkDnsServers(): List<String>? {
    val cm = getSystemService(ConnectivityManager::class.java) ?: return null
    val network = cm.activeNetwork ?: return null
    val lp = cm.getLinkProperties(network) ?: return null
    return lp.dnsServers.mapNotNull { it.hostAddress }
  }

  private fun errorMessage(error: NmapCommandBuilder.Result.Error): String =
    when (error.kind) {
      NmapCommandBuilder.ErrorKind.INVALID_SUDO_SYNTAX ->
        getString(R.string.invalid_sudo_syntax)
      NmapCommandBuilder.ErrorKind.INVALID_NMAP_SYNTAX ->
        getString(R.string.invalid_nmap_syntax)
      NmapCommandBuilder.ErrorKind.RESERVED_FLAG ->
        getString(R.string.reserved_nmap_flag_error, error.flag)
    }

  override fun onClick(view: View) {
    when (view.id) {
      R.id.scan_control_button -> {
        if (viewModel.isScanning.value == true) {
          viewModel.stopScan()
          return
        }
        val dnsServers = activeNetworkDnsServers()
        if (dnsServers == null) {
          Toast.makeText(this, R.string.no_active_network_error, Toast.LENGTH_LONG).show()
          return
        }
        val command = when (val result = buildCommand(dnsServers)) {
          is NmapCommandBuilder.Result.Success -> result.argv
          is NmapCommandBuilder.Result.Error -> {
            Toast.makeText(this, errorMessage(result), Toast.LENGTH_LONG).show()
            return
          }
        }
        cleanTmpFiles()
        Log.d(LOG_TAG, command.toString())
        viewModel.startScan(command)
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

  private fun hidePostScanButtons() {
    binding.parseOutputButton.visibility = View.GONE
    binding.clearOutputButton.visibility = View.GONE
  }

  private fun showPostScanButtons() {
    binding.clearOutputButton.visibility = View.VISIBLE
    val parsable = viewModel.lastScanOutcome.value == NmapScan.Outcome.COMPLETED &&
        File(filesDir, XML_OUTPUT_FILE).exists()
    if (isParserEnabled() && parsable) {
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
