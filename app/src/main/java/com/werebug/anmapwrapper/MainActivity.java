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
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements OnClickListener {

    private static class ReadNmapOutput implements Runnable {
        private final WeakReference<MainActivity> mainActivityRef;
        private final Handler mainThreadHandler;

        ReadNmapOutput(WeakReference<MainActivity> mainActivityRef, Handler mainThreadHandler) {
            this.mainActivityRef = mainActivityRef;
            this.mainThreadHandler = mainThreadHandler;
        }

        @Override
        public void run() {
            String nmapOutput = mainActivityRef.get().readNmapOutputStream();
            mainThreadHandler.post(() -> mainActivityRef.get().updateOutputView(nmapOutput));
        }
    }

    private static class StartNmapScan implements Runnable {
        private final WeakReference<MainActivity> mainActivityRef;
        private final ArrayList<String> argv;

        StartNmapScan(WeakReference<MainActivity> mainActivityRef, ArrayList<String> argv) {
            this.mainActivityRef = mainActivityRef;
            this.argv = argv;
        }

        @Override
        public void run() {
            mainActivityRef.get().startScan(argv.toArray(new String[0]));
        }
    }

    private static class StopNmapScan implements Runnable {
        private final WeakReference<MainActivity> mainActivityRef;
        private final Handler mainThreadHandler;

        StopNmapScan(WeakReference<MainActivity> mainActivityRef, Handler mainThreadHandler) {
            this.mainActivityRef = mainActivityRef;
            this.mainThreadHandler = mainThreadHandler;
        }

        @Override
        public void run() {
            mainActivityRef.get().stopScan();
            mainThreadHandler.post(() -> Toast.makeText(
                    mainActivityRef.get(), "Stopped.", Toast.LENGTH_SHORT).show());
        }
    }

    static {
        System.loadLibrary("nmap-wrapper-lib");
    }

    private static final String LOG_TAG = "ANMAPWRAPPER_CUSTOM_LOG";
    private static final String NMAP_END_TAG = "NMAP_END";
    private static final String[] NMAP_ASSETS = {"nmap-service-probes",
                                                 "nmap-services",
                                                 "nmap-protocols",
                                                 "nmap-rpc",
                                                 "nmap-mac-prefixes",
                                                 "nmap-os-db"};

    public native void startScan(String[] argv);
    public native void stopScan();
    public native String readNmapOutputStream();

    private ActivityMainBinding binding;
    private boolean isScanning = false;
    private final ExecutorService executorService = Executors.newFixedThreadPool(4);
    private final Handler mainThreadHandler = HandlerCompat.createAsync(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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

    private ArrayList<String> getNmapArgv() {
        ArrayList<String> argv = new ArrayList<>(Arrays.asList(String.valueOf(
                binding.nmapCommandInput.getText()).split(" ")));
        if (!argv.get(0).equals("nmap")) {
            argv.add(0, "nmap");
        }
        argv.add("--datadir");
        argv.add(String.valueOf(getFilesDir()));
        if (!argv.contains("--dns-servers")) {
            argv.add("--dns-servers");
            argv.add("8.8.8.8");
        }
        return argv;
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.scan_control_button) {
            if (isScanning) {
                executorService.execute(
                        new StopNmapScan(new WeakReference<>(this), mainThreadHandler));
                return;
            }
            ArrayList<String> argv = getNmapArgv();
            executorService.execute(new StartNmapScan(new WeakReference<>(this), argv));
            isScanning = true;
            binding.scanControlButton.setImageResource(android.R.drawable.ic_media_pause);
            binding.outputTextView.setText("");
            executorService.execute(
                    new ReadNmapOutput(new WeakReference<>(this), mainThreadHandler));
        }
    }

    private void updateOutputView(String retrieved_output) {
        if (retrieved_output.equals(NMAP_END_TAG)) {
            isScanning = false;
            binding.scanControlButton.setImageResource(android.R.drawable.ic_menu_send);
            return;
        }
        binding.outputTextView.setText(
                String.format("%s%s", binding.outputTextView.getText(), retrieved_output));
        executorService.execute(
                new ReadNmapOutput(new WeakReference<>(this), mainThreadHandler));
    }
}