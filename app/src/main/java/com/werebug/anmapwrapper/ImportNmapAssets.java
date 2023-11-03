package com.werebug.anmapwrapper;

import static com.werebug.anmapwrapper.MainActivity.LOG_TAG;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.ref.WeakReference;
import java.util.Objects;

public class ImportNmapAssets implements Runnable {

    private static final String ASSET_VERSION_PREFS_KEY = "last_installed_asset_version";
    private static final String ASSET_VERSION = "7.94";
    private static final String[] NMAP_ASSETS = {"nmap-service-probes",
                                                 "nmap-services",
                                                 "nmap-protocols",
                                                 "nmap-rpc",
                                                 "nmap-mac-prefixes",
                                                 "nmap-os-db"};
    private final WeakReference<MainActivity> mainActivityRef;
    private final SharedPreferences preferences;

    public ImportNmapAssets(WeakReference<MainActivity> mainActivityRef) {
        this.mainActivityRef = mainActivityRef;
        this.preferences = this.mainActivityRef.get().getPreferences(Context.MODE_PRIVATE);
    }

    @Override
    public void run() {
        String lastImportedVersion = preferences.getString(ASSET_VERSION_PREFS_KEY, "");
        for (String assetFileName : NMAP_ASSETS) {
            File assetFile = mainActivityRef.get().getFileStreamPath(assetFileName);
            if (Objects.requireNonNull(assetFile).exists()
                    && lastImportedVersion.equals(ASSET_VERSION)) {
                continue;
            }
            BufferedReader reader = null;
            BufferedWriter writer = null;
            try {
                reader = new BufferedReader(new InputStreamReader(
                        mainActivityRef.get().getAssets().open(assetFileName)));
                writer = new BufferedWriter(new OutputStreamWriter(
                        mainActivityRef.get().openFileOutput(assetFileName, Context.MODE_PRIVATE)));
                String line;
                while ((line = reader.readLine()) != null) {
                    writer.write(line + "\n");
                }
                Log.d(LOG_TAG, assetFileName + " imported.");
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error importing " + assetFileName + ".");
            } finally {
                try {
                    Objects.requireNonNull(reader).close();
                } catch (IOException ignored) {
                }
                try {
                    Objects.requireNonNull(writer).close();
                } catch (IOException ignored) {
                }
            }
        }
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(ASSET_VERSION_PREFS_KEY, ASSET_VERSION);
        editor.apply();
    }
}
