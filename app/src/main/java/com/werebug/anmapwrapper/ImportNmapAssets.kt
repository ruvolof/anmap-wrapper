package com.werebug.anmapwrapper

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.lang.ref.WeakReference

class ImportNmapAssets(private val mainActivityRef: WeakReference<MainActivity>) : Runnable {

    companion object {
        private const val ASSET_VERSION_PREFS_KEY = "last_installed_asset_version"
        private const val ASSET_VERSION = "7.96"
        private val NMAP_FILE_ASSETS = arrayOf(
            "nmap-service-probes",
            "nmap-services",
            "nmap-protocols",
            "nmap-rpc",
            "nmap-mac-prefixes",
            "nmap-os-db",
            "nse_main.lua"
        )
        private val NMAP_FOLDER_ASSETS = arrayOf("scripts", "nselib")
    }

    private val preferences: SharedPreferences =
        mainActivityRef.get()!!.getPreferences(Context.MODE_PRIVATE)
    private var lastImportedVersion: String? = null

    override fun run() {
        lastImportedVersion = preferences.getString(ASSET_VERSION_PREFS_KEY, "")
        for (dataAssetFile in NMAP_FILE_ASSETS) {
            copyAssetFileToInternalStorage(dataAssetFile, dataAssetFile)
        }
        for (assetFolder in NMAP_FOLDER_ASSETS) {
            copyAssetDirToInternalStorage(assetFolder, assetFolder)
        }
        val editor = preferences.edit()
        editor.putString(ASSET_VERSION_PREFS_KEY, ASSET_VERSION)
        editor.apply()
    }

    private fun copyAssetFileToInternalStorage(assetPath: String, targetPath: String) {
        val targetFile = File(mainActivityRef.get()!!.filesDir, targetPath)
        if (targetFile.exists() && lastImportedVersion == ASSET_VERSION) {
            return
        }
        var inputStream: InputStream? = null
        var outputStream: OutputStream? = null
        try {
            inputStream = mainActivityRef.get()!!.assets.open(assetPath)
            outputStream = FileOutputStream(targetFile)
            val buffer = ByteArray(1024)
            var length: Int
            while (inputStream.read(buffer).also { length = it } > 0) {
                outputStream.write(buffer, 0, length)
            }
            Log.i(MainActivity.LOG_TAG, "$assetPath successfully imported.")
        } catch (e: IOException) {
            Log.e(MainActivity.LOG_TAG, "Error importing $assetPath")
        } finally {
            inputStream?.close()
            outputStream?.flush()
            outputStream?.close()
        }
    }

    private fun copyAssetDirToInternalStorage(assetDir: String, targetDir: String) {
        val assets = mainActivityRef.get()!!.assets.list(assetDir) ?: return
        val targetDirectory = File(mainActivityRef.get()!!.filesDir, targetDir)
        if (!targetDirectory.exists()) {
            targetDirectory.mkdirs()
        }
        for (asset in assets) {
            val assetPath = if (assetDir.isEmpty()) asset else "$assetDir/$asset"
            val targetPath = "$targetDir/$asset"
            if (mainActivityRef.get()!!.assets.list(assetPath)?.isNotEmpty() == true) {
                // If the asset is a directory, recurse into it
                copyAssetDirToInternalStorage(assetPath, targetPath)
            } else {
                copyAssetFileToInternalStorage(assetPath, targetPath)
            }
        }
    }


}