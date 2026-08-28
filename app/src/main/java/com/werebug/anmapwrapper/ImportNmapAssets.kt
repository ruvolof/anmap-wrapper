package com.werebug.anmapwrapper

import android.content.SharedPreferences
import android.content.res.AssetManager
import android.util.Log
import androidx.core.content.edit
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class ImportNmapAssets(
  private val assets: AssetManager,
  private val filesDir: File,
  private val preferences: SharedPreferences
) : Runnable {

  companion object {
    private const val ASSET_VERSION_PREFS_KEY = "last_installed_asset_version"
    private const val ASSET_VERSION = "7.991"
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
    var allSucceeded = true
    for (dataAssetFile in NMAP_FILE_ASSETS) {
      if (!copyAssetFileToInternalStorage(dataAssetFile, dataAssetFile, lastImportedVersion)) {
        allSucceeded = false
      }
    }
    for (assetFolder in NMAP_FOLDER_ASSETS) {
      if (!copyAssetDirToInternalStorage(assetFolder, assetFolder, lastImportedVersion)) {
        allSucceeded = false
      }
    }
    if (allSucceeded) {
      preferences.edit {
        putString(ASSET_VERSION_PREFS_KEY, ASSET_VERSION)
      }
    } else {
      Log.e(MainActivity.LOG_TAG, "Asset import incomplete; version pref not updated")
    }
  }

  private fun copyAssetFileToInternalStorage(
    assetPath: String,
    targetPath: String,
    lastImportedVersion: String?
  ): Boolean {
    val targetFile = File(filesDir, targetPath)
    if (targetFile.exists() && lastImportedVersion == ASSET_VERSION) {
      return true
    }
    return try {
      assets.open(assetPath).use { input ->
        FileOutputStream(targetFile).use { output ->
          input.copyTo(output)
        }
      }
      Log.i(MainActivity.LOG_TAG, "$assetPath successfully imported.")
      true
    } catch (e: IOException) {
      Log.e(MainActivity.LOG_TAG, "Error importing $assetPath: ${e.message}")
      false
    }
  }

  private fun copyAssetDirToInternalStorage(
    assetDir: String,
    targetDir: String,
    lastImportedVersion: String?
  ): Boolean {
    val children = assets.list(assetDir)
    if (children == null) {
      Log.e(MainActivity.LOG_TAG, "Error listing asset dir $assetDir")
      return false
    }
    val targetDirectory = File(filesDir, targetDir)
    if (!targetDirectory.exists() && !targetDirectory.mkdirs()) {
      Log.e(MainActivity.LOG_TAG, "Error creating directory ${targetDirectory.path}")
      return false
    }
    var allSucceeded = true
    for (asset in children) {
      val assetPath = if (assetDir.isEmpty()) asset else "$assetDir/$asset"
      val targetPath = "$targetDir/$asset"
      val ok = if (assets.list(assetPath)?.isNotEmpty() == true) {
        copyAssetDirToInternalStorage(assetPath, targetPath, lastImportedVersion)
      } else {
        copyAssetFileToInternalStorage(assetPath, targetPath, lastImportedVersion)
      }
      if (!ok) allSucceeded = false
    }
    return allSucceeded
  }
}
