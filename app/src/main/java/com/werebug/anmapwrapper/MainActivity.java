package com.werebug.anmapwrapper;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.os.HandlerCompat;

import com.werebug.anmapwrapper.databinding.ActivityMainBinding;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements OnClickListener {

    public static final String LOG_TAG = "ANMAPWRAPPER_CUSTOM_LOG";
    private static final String[] NMAP_ASSETS = {"nmap-service-probes",
                                                 "nmap-services",
                                                 "nmap-protocols",
                                                 "nmap-rpc",
                                                 "nmap-mac-prefixes",
                                                 "nmap-os-db"};
    private final ExecutorService executorService = Executors.newFixedThreadPool(1);
    private final Handler mainThreadHandler = HandlerCompat.createAsync(Looper.getMainLooper());

    private ActivityMainBinding binding;
    private String libDir;
    private String nmapExecutablePath;
    private boolean isScanning = false;
    private NmapScan currentNmapScan;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        libDir = getApplicationInfo().nativeLibraryDir;
        nmapExecutablePath = libDir + "/libnmap-wrapper.so";

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.scanControlButton.setOnClickListener(this);

        checkAndInstallNmapAssets();
    }

    private void checkAndInstallNmapAssets() {
        for (String nmapAsset : NMAP_ASSETS) {
            File nmap_services = getFileStreamPath(nmapAsset);
            if (Objects.requireNonNull(nmap_services).exists()) {
                continue;
            }
            BufferedReader asset_reader = null;
            BufferedWriter local_copy_writer = null;
            try {
                asset_reader = new BufferedReader(
                        new InputStreamReader(getAssets().open(nmapAsset)));
                local_copy_writer = new BufferedWriter(
                        new OutputStreamWriter(openFileOutput(nmapAsset, Context.MODE_PRIVATE)));
                String line;
                while ((line = asset_reader.readLine()) != null) {
                    local_copy_writer.write(line + "\n");
                }
                Log.d(LOG_TAG, nmapAsset + " imported.");
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error importing " + nmapAsset + ".");
            } finally {
                try {
                    Objects.requireNonNull(asset_reader).close();
                } catch (IOException ignored) { }
                try {
                    Objects.requireNonNull(local_copy_writer).close();
                } catch (IOException ignored) { }
            }
        }
    }

    private ArrayList<String> getNmapCommand() throws Exception {
        ArrayList<String> argv = new ArrayList<>(Arrays.asList(String.valueOf(
                binding.nmapCommandInput.getText()).split(" ")));
        int isSudo = argv.indexOf("sudo");
        if (isSudo > 0) {
            throw new Exception(getString(R.string.invalid_sudo_syntax));
        }
        if (isSudo == 0) {
            argv.remove(0);
            argv.addAll(0, Arrays.asList(new String[]{"su", "-c"}));
        }
        int nmapIndex = argv.indexOf("nmap");
        if ((isSudo == -1 && nmapIndex != 0) || (isSudo == 0 && nmapIndex != 2)) {
            throw new Exception(getString(R.string.invalid_nmap_syntax));
        }
        argv.remove(nmapIndex);
        argv.add(nmapIndex, nmapExecutablePath);
        Collections.addAll(argv, "--datadir", String.valueOf(getFilesDir()));
        if (!argv.contains("--dns-servers")) {
            Collections.addAll(argv, "--dns-servers", "8.8.8.8");
        }
        return argv;
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.scan_control_button) {
            if (isScanning) {
                currentNmapScan.stopScan();
                return;
            }
            ArrayList<String> command;
            try {
                command = getNmapCommand();
            } catch (Exception e) {
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                return;
            }
            Log.d(LOG_TAG, String.valueOf(command));
            currentNmapScan = new NmapScan(
                    new WeakReference<>(this), command, mainThreadHandler, libDir);
            executorService.execute(currentNmapScan);
        }
    }

    public void initScanView() {
        isScanning = true;
        binding.scanControlButton.setImageResource(android.R.drawable.ic_media_pause);
        binding.outputTextView.setText("");
    }

    public void updateOutputView(String retrieved_output, boolean finished) {
        if (finished) {
            isScanning = false;
            currentNmapScan = null;
            binding.scanControlButton.setImageResource(android.R.drawable.ic_menu_send);
        }
        binding.outputTextView.setText(
                String.format("%s%s", binding.outputTextView.getText(), retrieved_output));
    }
}