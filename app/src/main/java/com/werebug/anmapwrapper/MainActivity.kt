package com.werebug.anmapwrapper

import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.HandlerCompat
import com.werebug.anmapwrapper.databinding.ActivityMainBinding
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
        executorService.execute(ImportNmapAssets(WeakReference(this)))
    }

    private fun getNmapCommandArguments(): List<String> {
        val argv = binding.nmapCommandInput.text.toString().split(" ").toMutableList()
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
        Collections.addAll(argv, "--datadir", filesDir.toString())
        if (!argv.contains("--dns-servers")) {
            Collections.addAll(argv, "--dns-servers", "8.8.8.8")
        }
        return argv
    }

    override fun onClick(view: View) {
        if (view.id == R.id.scan_control_button) {
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
    }

    fun initScanView() {
        isScanning = true
        binding.scanControlButton.setImageResource(android.R.drawable.ic_media_pause)
        binding.outputTextView.text = ""
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
}