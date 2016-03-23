package org.nmap.anmap;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;

public class ANmap extends AppCompatActivity implements View.OnClickListener {

    private class AsyncNmapExecutor extends AsyncTask<ArrayList<String>, Void, String> {

        @Override
        protected String doInBackground(ArrayList<String>... argv) {
            Log.d(LOG_NMAP, getResources().getString(R.string.nmap_argv_creation));
            for (String s : argv[0]) {
                Log.d(LOG_NMAP, s);
            }
            String raw_results = ANmap.this.nmapWrapper(argv[0].toArray(new String[0]));
            ArrayList<String> raw_lines = new ArrayList<String>(Arrays.asList(raw_results.split("\n")));
            ArrayList<String> a_results = new ArrayList<String>();
            for (int i = 0; i < raw_lines.size(); i++) {
                if (raw_lines.get(i).startsWith(TRASH_NDK_STDOUT_PREFIX)) {
                    Log.d(LOG_NMAP, getResources().getString(R.string.trash_stdout_remove) + raw_lines.get(i));
                    continue;
                }
                a_results.add(raw_lines.get(i));
            }
            String result = TextUtils.join("\n", a_results);
            return result;
        }

        protected void onPostExecute(String result) {
            ANmap.this.scan_results_view.setText(result);
        }
    }

    public final static String SERVICE_FILE_NAME = "nmap-services";
    public final static String WRAPPER_LIBRARY = "nmap-wrapper";
    public final static String LOG_NMAP = "ANMAP LOG";
    public final static String TRASH_NDK_STDOUT_PREFIX = "referenceTable";
    public final static String DEFAULT_DNS_SERVER = "8.8.8.8";

    // Layout widget
    private Button scan_button;
    private TextView scan_results_view;
    private EditText host_set;

    // Native functions
    public native String nmapWrapper(String[] argv);

    static {
        System.loadLibrary(WRAPPER_LIBRARY);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_anmap);

        // Retrieve layout widgets and set properties
        scan_button = (Button) findViewById(R.id.scan_button);
        scan_results_view = (TextView) findViewById(R.id.scan_results_view);
        host_set = (EditText) findViewById(R.id.host_set);

        scan_button.setOnClickListener(this);

        // Checking if nmap-services was already stored in internal storage
        checkAndInstallNmapServices();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

    }

    private void checkAndInstallNmapServices() {
        try {
            openFileInput(SERVICE_FILE_NAME);
            Log.d(LOG_NMAP, getResources().getString(R.string.nmap_services_found));
        } catch (FileNotFoundException e) {
            Log.d(LOG_NMAP, getResources().getString(R.string.namp_services_not_found));
            BufferedReader a = null;
            BufferedWriter f = null;
            try {
                a = new BufferedReader(new InputStreamReader(getAssets().open(SERVICE_FILE_NAME)));
                f = new BufferedWriter(new OutputStreamWriter(openFileOutput(SERVICE_FILE_NAME, Context.MODE_PRIVATE)));

                String line;
                while ((line = a.readLine()) != null) {
                    f.write(line + "\n");
                }
                Log.d(LOG_NMAP, getResources().getString(R.string.nmap_services_stored));
            } catch (IOException e1) {
                Log.e(LOG_NMAP, getResources().getString(R.string.nmap_services_no_assets));
            }   finally {
                try {
                    a.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                try {
                    f.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.scan_button:
                scan_results_view.setText("");
                String param = host_set.getText().toString();
                if (param.equals("")) {
                    Toast.makeText(this, R.string.no_host_set, Toast.LENGTH_SHORT).show();
                } else {
                    ArrayList<String> argv = new ArrayList<String>(Arrays.asList(param.split(" ")));
                    argv.add(0, "nmap");
                    argv.add("--servicedb");
                    argv.add(getFilesDir() + "/" + SERVICE_FILE_NAME);
                    argv.add("--dns-server");
                    argv.add(DEFAULT_DNS_SERVER);
                    new AsyncNmapExecutor().execute(argv);
                }
        }
    }
}
