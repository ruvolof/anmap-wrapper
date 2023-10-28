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
        nmapExecutablePath = libDir + "/libnmap.so";

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.scanControlButton.setOnClickListener(this);

        executorService.execute(new ImportNmapAssets(new WeakReference<>(this)));
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