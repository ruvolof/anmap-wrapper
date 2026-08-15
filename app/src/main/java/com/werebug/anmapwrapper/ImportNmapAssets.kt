package com.werebug.anmapwrapper

import android.content.SharedPreferences
import android.content.res.AssetManager
import android.util.Log
import androidx.core.content.edit
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

class ImportNmapAssets(
  private val assets: AssetManager,
  private val filesDir: File,
  private val preferences: SharedPreferences
) : Runnable {

  companion object {
    private const val ASSET_VERSION_PREFS_KEY = "last_installed_asset_version"
    private const val ASSET_VERSION = "7.99"
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

  override fun run() {
    val lastImportedVersion = preferences.getString(ASSET_VERSION_PREFS_KEY, "")
    for (dataAssetFile in NMAP_FILE_ASSETS) {
      copyAssetFileToInternalStorage(dataAssetFile, dataAssetFile, lastImportedVersion)
    }
    for (assetFolder in NMAP_FOLDER_ASSETS) {
      copyAssetDirToInternalStorage(assetFolder, assetFolder, lastImportedVersion)
    }
    preferences.edit {
      putString(ASSET_VERSION_PREFS_KEY, ASSET_VERSION)
    }
  }

  private fun copyAssetFileToInternalStorage(
    assetPath: String,
    targetPath: String,
    lastImportedVersion: String?
  ) {
    val targetFile = File(filesDir, targetPath)
    if (targetFile.exists() && lastImportedVersion == ASSET_VERSION) {
      return
    }
    var inputStream: InputStream? = null
    var outputStream: OutputStream? = null
    try {
      inputStream = assets.open(assetPath)
      outputStream = FileOutputStream(targetFile)
      val buffer = ByteArray(1024)
      var length: Int
      while (inputStream.read(buffer).also { length = it } > 0) {
        outputStream.write(buffer, 0, length)
      }
      Log.i(MainActivity.LOG_TAG, "$assetPath successfully imported.")
    } catch (_: IOException) {
      Log.e(MainActivity.LOG_TAG, "Error importing $assetPath")
    } finally {
      inputStream?.close()
      outputStream?.flush()
      outputStream?.close()
    }
  }

  private fun copyAssetDirToInternalStorage(
    assetDir: String,
    targetDir: String,
    lastImportedVersion: String?
  ) {
    val children = assets.list(assetDir) ?: return
    val targetDirectory = File(filesDir, targetDir)
    if (!targetDirectory.exists()) {
      targetDirectory.mkdirs()
    }
    for (asset in children) {
      val assetPath = if (assetDir.isEmpty()) asset else "$assetDir/$asset"
      val targetPath = "$targetDir/$asset"
      if (assets.list(assetPath)?.isNotEmpty() == true) {
        // Directory, recurse into it
        copyAssetDirToInternalStorage(assetPath, targetPath, lastImportedVersion)
      } else {
        copyAssetFileToInternalStorage(assetPath, targetPath, lastImportedVersion)
      }
    }
  }
}
