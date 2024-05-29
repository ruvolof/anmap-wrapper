package com.werebug.anmapwrapper

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.lang.ref.WeakReference

class ImportNmapAssets(private val mainActivityRef: WeakReference<MainActivity>) : Runnable {

    companion object {
        private const val ASSET_VERSION_PREFS_KEY = "last_installed_asset_version"
        private const val ASSET_VERSION = "7.95"
        private val NMAP_ASSETS = arrayOf(
            "nmap-service-probes",
            "nmap-services",
            "nmap-protocols",
            "nmap-rpc",
            "nmap-mac-prefixes",
            "nmap-os-db"
        )
    }

    private val preferences: SharedPreferences =
        mainActivityRef.get()!!.getPreferences(Context.MODE_PRIVATE)

    override fun run() {
        val lastImportedVersion = preferences.getString(ASSET_VERSION_PREFS_KEY, "")
        for (assetFileName in NMAP_ASSETS) {
            val assetFile = mainActivityRef.get()!!.getFileStreamPath(assetFileName)
            if (assetFile.exists() && lastImportedVersion == ASSET_VERSION) {
                continue
            }
            var reader: BufferedReader? = null
            var writer: BufferedWriter? = null
            try {
                reader = BufferedReader(
                    InputStreamReader(
                        mainActivityRef.get()!!.assets.open(assetFileName)
                    )
                )
                writer = BufferedWriter(
                    OutputStreamWriter(
                        mainActivityRef.get()!!.openFileOutput(
                            assetFileName, Context.MODE_PRIVATE
                        )
                    )
                )
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    writer.write("$line\n")
                }
                Log.d(MainActivity.LOG_TAG, "$assetFileName imported.")
            } catch (e: IOException) {
                Log.e(MainActivity.LOG_TAG, "Error importing $assetFileName.")
            } finally {
                reader?.close()
                writer?.close()
            }
        }
        val editor = preferences.edit()
        editor.putString(ASSET_VERSION_PREFS_KEY, ASSET_VERSION)
        editor.apply()
    }


}