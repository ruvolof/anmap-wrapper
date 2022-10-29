package com.werebug.anmapwrapper;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

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

public class MainActivity extends AppCompatActivity implements OnClickListener {

    private static abstract class InnerAsyncTask<Input, Progress, Result>
            extends AsyncTask<Input, Progress, Result> {
        public WeakReference<MainActivity> mainActivityRef;

        InnerAsyncTask(MainActivity context) {
            mainActivityRef = new WeakReference<>(context);
        }
    }

    private static class ReadNmapOutput extends InnerAsyncTask<String, String, String> {
        ReadNmapOutput(MainActivity context) {
            super(context);
        }

        @Override
        protected String doInBackground(String... strings) {
            return mainActivityRef.get().readNmapOutputStream();
        }

        @Override
        protected void onPostExecute(String retrieved_output) {
            mainActivityRef.get().updateOutputView(retrieved_output);
        }
    }

    private static class StartNmapScan extends InnerAsyncTask<ArrayList<String>, Void, Void> {
        StartNmapScan(MainActivity context) {
            super(context);
        }

        @Override
        protected Void doInBackground(ArrayList<String>... argv) {
            mainActivityRef.get().startScan(argv[0].toArray(new String[0]));
            return null;
        }
    }

    private static class StopNmapScan extends InnerAsyncTask<Void, Void, Void> {
        StopNmapScan(MainActivity context) {
            super(context);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            mainActivityRef.get().stopScan();
            return null;
        }

        @Override
        protected void onPostExecute(Void v) {
            Toast.makeText(mainActivityRef.get(), "Stopped.", Toast.LENGTH_SHORT).show();
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
                } catch (IOException ignored) { }
                try {
                    Objects.requireNonNull(local_copy_writer).close();
                } catch (IOException ignored) { }
            }
        }
    }

    private ArrayList<String> getNmapArgv() {
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
        return argv;
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.scan_control_button) {
            if (isScanning) {
                new StopNmapScan(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                return;
            }
            ArrayList<String> argv = getNmapArgv();
            new StartNmapScan(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, argv);
            isScanning = true;
            binding.scanControlButton.setImageResource(android.R.drawable.ic_media_pause);
            binding.outputTextView.setText("");
            new ReadNmapOutput(this).execute();
        }
    }

    private void updateOutputView(String retrieved_output) {
        if (retrieved_output.equals("NMAP_END")) {
            isScanning = false;
            binding.scanControlButton.setImageResource(android.R.drawable.ic_menu_send);
            return;
        }
        binding.outputTextView.setText(
                String.format("%s%s", binding.outputTextView.getText(), retrieved_output));
        new ReadNmapOutput(this).execute();
    }
}