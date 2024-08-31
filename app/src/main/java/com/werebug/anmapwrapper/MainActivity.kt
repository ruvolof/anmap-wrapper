package com.werebug.anmapwrapper

import android.content.Intent
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.HandlerCompat
import com.werebug.anmapwrapper.databinding.ActivityMainBinding
import com.werebug.anmapwrapper.parser.ParserActivity
import java.io.File
import java.lang.ref.WeakReference
import java.util.Collections
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity(), View.OnClickListener {

    companion object {
        const val LOG_TAG = "ANMAPWRAPPER_CUSTOM_LOG"
    }

    private val executorService = Executors.newFixedThreadPool(1)
    private val mainThreadHandler = HandlerCompat.createAsync(Looper.getMainLooper())
    private lateinit var binding: ActivityMainBinding
    private lateinit var libDir: String
    private lateinit var nmapExecutablePath: String
    private var isScanning = false
    private var currentNmapScan: NmapScan? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        libDir = applicationInfo.nativeLibraryDir
        nmapExecutablePath = "$libDir/libnmap.so"
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.scanControlButton.setOnClickListener(this)
        binding.parseOutputButton.setOnClickListener(this)
        executorService.execute(ImportNmapAssets(WeakReference(this)))
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
        val argv = binding.nmapCommandInput.text.toString().split(" ").toMutableList()
        patchBinaryPaths(argv)
        patchReserved(argv, "--datadir", filesDir.toString())
        patchReserved(argv, "-oX", File(filesDir, "scan_output.xml").path)
        patchDefault(argv, "--dns-servers", "8.8.8.8")
        return argv
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.scan_control_button -> {
                if (isScanning) {
                    currentNmapScan!!.stopScan()
                    return
                }
                val command = try {
                    getNmapCommandArguments()
                } catch (e: Exception) {
                    Toast.makeText(this, e.message, Toast.LENGTH_LONG).show()
                    return
                }
                Log.d(LOG_TAG, command.toString())
                currentNmapScan = NmapScan(
                    WeakReference(this), command, mainThreadHandler, libDir
                )
                executorService.execute(currentNmapScan)
            }

            R.id.parse_output_button -> {
                startParserActivity()
            }
        }
    }

    fun initScanView() {
        isScanning = true
        binding.scanControlButton.setImageResource(android.R.drawable.ic_media_pause)
        binding.outputTextView.text = ""
        binding.parseOutputButton.visibility = View.GONE
    }

    fun updateOutputView(retrievedOutput: String?, finished: Boolean) {
        if (finished) {
            isScanning = false
            currentNmapScan = null
            binding.scanControlButton.setImageResource(android.R.drawable.ic_menu_send)
        }
        binding.outputTextView.text = String.format(
            "%s%s", binding.outputTextView.text, retrievedOutput
        )
    }

    fun showParseButton() {
        binding.parseOutputButton.visibility = View.VISIBLE
    }

    private fun startParserActivity() {
        val intent = Intent(this, ParserActivity::class.java)
        startActivity(intent)
    }
}