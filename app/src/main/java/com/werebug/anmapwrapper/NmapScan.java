package com.werebug.anmapwrapper;

import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Map;

public class NmapScan implements Runnable {
    private final WeakReference<MainActivity> mainActivityRef;
    private final Handler mainThreadHandler;
    private final ArrayList<String> command;
    private final String libDir;
    private boolean stopped = false;

    NmapScan(WeakReference<MainActivity> mainActivityRef,
             ArrayList<String> command,
             Handler mainThreadHandler,
             String libDir) {
        this.mainActivityRef = mainActivityRef;
        this.command = command;
        this.mainThreadHandler = mainThreadHandler;
        this.libDir = libDir;
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
                            () -> safeUpdateOutputView(
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
            mainThreadHandler.post(() -> safeUpdateOutputView("", true));
        } catch (IOException e) {
            Log.e(MainActivity.LOG_TAG, e.getMessage());
            mainThreadHandler.post(
                    () -> Toast.makeText(
                            mainActivityRef.get(), e.getMessage(), Toast.LENGTH_LONG).show());
        }
    }

    private void safeUpdateOutputView(String message, boolean finished) {
        MainActivity mainActivity = mainActivityRef.get();
        if (mainActivity != null) {
            mainActivity.updateOutputView(message, finished);
        }
    }

    public void stopScan() {
        stopped = true;
    }
}
