package com.werebug.anmapwrapper;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;

import androidx.appcompat.app.AppCompatActivity;

import com.werebug.anmapwrapper.databinding.ActivityMainBinding;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

public class MainActivity extends AppCompatActivity implements OnClickListener {

    private class ReadNmapOutput extends AsyncTask<String, String, String> {
        @Override
        protected String doInBackground(String... strings) {
            String retrieved_output = MainActivity.this.readNmapOutputStream();
            return retrieved_output;
        }

        protected void onPostExecute(String retrieved_output) {
            MainActivity.this.updateOutputView(retrieved_output);
        }
    }

    private class StartNmapScan extends AsyncTask<ArrayList<String>, String, String> {
        @Override
        protected String doInBackground(ArrayList<String>... argv) {
            MainActivity.this.startScan(argv[0].toArray(new String[0]));
            return null;
        }
    }

    static {
        System.loadLibrary("nmap-wrapper-lib");
    }

    private ActivityMainBinding binding;
    private static final String LOG_TAG = "ANMAPWRAPPER_CUSTOM_LOG";
    private static final String[] NMAP_ASSETS = {"nmap-service-probes",
                                                 "nmap-services",
                                                 "nmap-protocols",
                                                 "nmap-rpc",
                                                 "nmap-mac-prefixes",
                                                 "nmap-os-db"};

    public native void startScan(String[] argv);
    public native String readNmapOutputStream();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.startScanButton.setOnClickListener(this);

        checkAndInstallNmapAssets();
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.start_scan_button) {
            ArrayList<String> argv = new ArrayList<String>(Arrays.asList(String.valueOf(
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
            new StartNmapScan().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, argv);
            binding.outputTextView.setText("");
            new ReadNmapOutput().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    private void updateOutputView(String retrieved_output) {
        if (!retrieved_output.equals("NMAP_END")) {
            binding.outputTextView.setText(
                    String.format("%s%s", binding.outputTextView.getText(), retrieved_output));
            new ReadNmapOutput().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
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
                String line = null;
                while ((line = asset_reader.readLine()) != null) {
                    local_copy_writer.write(line + "\n");
                }
                Log.d(LOG_TAG, nmapAsset + " imported.");
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error importing " + nmapAsset + ".");
            } finally {
                try {
                    Objects.requireNonNull(asset_reader).close();
                } catch (IOException e) { }
                try {
                    Objects.requireNonNull(local_copy_writer).close();
                } catch (IOException e) { }
            }
        }
    }
}