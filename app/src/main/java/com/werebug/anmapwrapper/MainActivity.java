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
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.ref.WeakReference;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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
            String fifoPath = mainActivityRef.get().fifoPath;
            BufferedReader pipeReader = null;
            try {
                pipeReader = new BufferedReader(new FileReader(fifoPath));
                String line = pipeReader.readLine();
                while (line != null) {
                    String retrievedOutput = line + "\n";
                    mainThreadHandler.post(
                            () -> mainActivityRef.get().updateOutputView(retrievedOutput, false));
                    line = pipeReader.readLine();
                }
                pipeReader.close();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    Objects.requireNonNull(pipeReader).close();
                } catch (IOException ignored) {}
                mainActivityRef.get().cleanUpFifo();
            }
            mainActivityRef.get().updateOutputView("", true);
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
            mainActivityRef.get().cleanUpFifo();
            try {
                Os.mkfifo(mainActivityRef.get().fifoPath,
                          OsConstants.S_IRUSR | OsConstants.S_IWUSR);
            } catch (ErrnoException e) {
                e.printStackTrace();
                return;
            }
            mainActivityRef.get().startScan(argv.toArray(new String[0]),
                                            mainActivityRef.get().fifoPath);
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

    private static final String FIFO_NAME = "nmap_output_fifo";
    private static final String LOG_TAG = "ANMAPWRAPPER_CUSTOM_LOG";
    private static final String NMAP_END_TAG = "NMAP_END";
    private static final String[] NMAP_ASSETS = {"nmap-service-probes",
                                                 "nmap-services",
                                                 "nmap-protocols",
                                                 "nmap-rpc",
                                                 "nmap-mac-prefixes",
                                                 "nmap-os-db"};

    public native void startScan(String[] argv, String fifoPath);
    public native void stopScan();

    private ActivityMainBinding binding;
    private boolean isScanning = false;
    private String fifoPath = null;
    private final ExecutorService executorService = Executors.newFixedThreadPool(4);
    private final Handler mainThreadHandler = HandlerCompat.createAsync(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.scanControlButton.setOnClickListener(this);

        fifoPath = String.valueOf(getFilesDir()) + "/" + FIFO_NAME;
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
                executorService.execute(
                        new StopNmapScan(new WeakReference<>(this), mainThreadHandler));
                return;
            }
            ArrayList<String> argv = getNmapArgv();
            Log.d(LOG_TAG, String.valueOf(argv));
            executorService.execute(
                    new StartNmapScan(new WeakReference<>(this), argv));
            isScanning = true;
            binding.scanControlButton.setImageResource(android.R.drawable.ic_media_pause);
            binding.outputTextView.setText("");
            executorService.execute(
                    new ReadNmapOutput(new WeakReference<>(this), mainThreadHandler));
        }
    }

    private void updateOutputView(String retrieved_output, boolean finished) {
        if (finished) {
            isScanning = false;
            binding.scanControlButton.setImageResource(android.R.drawable.ic_menu_send);
        }
        binding.outputTextView.setText(
                String.format("%s%s", binding.outputTextView.getText(), retrieved_output));
    }

    private void cleanUpFifo() {
        File fifo = new File(fifoPath);
        if (fifo.exists()) {
            fifo.delete();
        }
    }
}