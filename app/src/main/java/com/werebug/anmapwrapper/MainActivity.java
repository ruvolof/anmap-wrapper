package com.werebug.anmapwrapper;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
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

    static {
        System.loadLibrary("nmap-wrapper-lib");
    }

    private ActivityMainBinding binding;
    private static final String NMAP_SERVICES_FILE = "nmap-services";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.startScanButton.setOnClickListener(this);

        checkAndInstallNmapServices();
    }

    public native void startScan(String[] argv);
    public native String readNmapOutputStream();

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.start_scan_button) {
            ArrayList<String> argv = new ArrayList<String>(Arrays.asList(String.valueOf(
                    binding.nmapCommandInput.getText()).split(" ")));
            argv.add("--datadir");
            argv.add(String.valueOf(getFilesDir()));
            if (!argv.contains("--dns-servers")) {
                argv.add("--dns-servers");
                argv.add("8.8.8.8");
            }
            startScan(argv.toArray(new String[0]));
            binding.outputTextView.setText("");
            new ReadNmapOutput().execute();
        }
    }

    private void updateOutputView(String retrieved_output) {
        if (!retrieved_output.equals("NMAP_END")) {
            binding.outputTextView.setText(binding.outputTextView.getText() + retrieved_output);
            new ReadNmapOutput().execute();
        }
    }

    private void checkAndInstallNmapServices() {
        File nmap_services = getFileStreamPath(NMAP_SERVICES_FILE);
        if (Objects.requireNonNull(nmap_services).exists()) {
            return;
        }
        BufferedReader nmap_services_asset = null;
        BufferedWriter nmap_services_local = null;
        try {
            nmap_services_asset = new BufferedReader(
                    new InputStreamReader(getAssets().open(NMAP_SERVICES_FILE)));
            nmap_services_local = new BufferedWriter(
                    new OutputStreamWriter(openFileOutput(NMAP_SERVICES_FILE,
                                                          Context.MODE_PRIVATE)));
            String line = null;
            while ((line = nmap_services_asset.readLine()) != null) {
                nmap_services_local.write(line + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                Objects.requireNonNull(nmap_services_asset).close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                Objects.requireNonNull(nmap_services_local).close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
}