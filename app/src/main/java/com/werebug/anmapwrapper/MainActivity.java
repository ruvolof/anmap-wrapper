package com.werebug.anmapwrapper;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
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
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class MainActivity extends AppCompatActivity implements OnClickListener {

    private static class StartNmapScan implements Runnable {
        private final WeakReference<MainActivity> mainActivityRef;
        private final Handler mainThreadHandler;
        private final ArrayList<String> command;
        private final String libDir;
        private boolean stopped = false;

        StartNmapScan(WeakReference<MainActivity> mainActivityRef,
                      Handler mainThreadHandler,
                      ArrayList<String> command) {
            this.mainActivityRef = mainActivityRef;
            this.mainThreadHandler = mainThreadHandler;
            this.command = command;
            this.libDir = this.mainActivityRef.get().libDir;
        }

        @Override
        public void run() {
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            Map<String, String> processEnv = processBuilder.environment();
            processEnv.put("LD_LIBRARY_PATH", libDir);
            processBuilder.redirectErrorStream(true);
            try {
                Process process = processBuilder.start();
                mainThreadHandler.post(() -> mainActivityRef.get().initScanView());
                InputStream processStdout = process.getInputStream();
                boolean exited = false;
                while (!exited && !stopped) {
                    int outputByteCount = processStdout.available();
                    if (outputByteCount > 0) {
                        byte[] bytes = new byte[outputByteCount];
                        processStdout.read(bytes);
                        mainThreadHandler.post(
                                () -> mainActivityRef.get().updateOutputView(
                                        new String(bytes, StandardCharsets.UTF_8), false)
                        );
                    }
                    if (stopped) {
                        process.destroy();
                    }
                    try {
                        process.exitValue();
                        exited = true;
                    } catch (IllegalThreadStateException ignored) {}
                }
                if (stopped) {
                    mainThreadHandler.post(
                            () -> Toast.makeText(
                                    mainActivityRef.get(), "Stopped.", Toast.LENGTH_SHORT).show());
                }
                mainThreadHandler.post(
                        () -> mainActivityRef.get().updateOutputView("", true));
            } catch (IOException e) {
                Log.e(LOG_TAG, e.getMessage());
            }
        }

        public void stopScan() {
            stopped = true;
        }
    }

    private static final String LOG_TAG = "ANMAPWRAPPER_CUSTOM_LOG";
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
    private StartNmapScan currentNmapScan;

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

    private ArrayList<String> getNmapCommand() {
        ArrayList<String> argv = new ArrayList<>(Arrays.asList(String.valueOf(
                binding.nmapCommandInput.getText()).split(" ")));
        if (argv.get(0).equals("nmap")) {
            argv.remove(0);
        }
        argv.add(0, nmapExecutablePath);
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
            ArrayList<String> command = getNmapCommand();
            Log.d(LOG_TAG, String.valueOf(command));
            currentNmapScan = new StartNmapScan(
                    new WeakReference<>(this), mainThreadHandler, command);
            executorService.execute(currentNmapScan);
        }
    }

    private void initScanView() {
        isScanning = true;
        binding.scanControlButton.setImageResource(android.R.drawable.ic_media_pause);
        binding.outputTextView.setText("");
    }

    private void updateOutputView(String retrieved_output, boolean finished) {
        if (finished) {
            isScanning = false;
            currentNmapScan = null;
            binding.scanControlButton.setImageResource(android.R.drawable.ic_menu_send);
        }
        binding.outputTextView.setText(
                String.format("%s%s", binding.outputTextView.getText(), retrieved_output));
    }
}