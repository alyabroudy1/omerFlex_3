package com.omerflex.service;

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import java.io.File; // Required for File operations

public class UpdateWorker extends Worker {

    private static final String TAG = "UpdateWorker";
    public static final String KEY_APK_URL = "apk_url";
    public static final String KEY_APK_NAME = "apk_name"; // e.g., "omerFlex_v123.apk"
    public static final String KEY_DOWNLOADED_APK_URI = "downloaded_apk_uri"; // Will store absolute path as String

    public UpdateWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        String apkUrl = getInputData().getString(KEY_APK_URL);
        String apkName = getInputData().getString(KEY_APK_NAME);

        if (apkUrl == null || apkUrl.isEmpty() || apkName == null || apkName.isEmpty()) {
            Log.e(TAG, "APK URL or Name not provided to UpdateWorker.");
            return Result.failure();
        }

        // Ensure target directory exists
        File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        if (!downloadDir.exists()) {
            if (!downloadDir.mkdirs()) {
                Log.e(TAG, "Failed to create download directory: " + downloadDir.getAbsolutePath());
                // Check if the path is writable or if there's another issue.
                // For now, just fail if mkdirs() fails.
                return Result.failure();
            }
        }
        File apkFile = new File(downloadDir, apkName);

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(apkUrl));
        request.setTitle("OmerFlex App Update");
        request.setDescription("Downloading " + apkName + "...");
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

        // Using setDestinationUri with a file URI.
        // DownloadManager handles writing to this file path.
        Uri destinationUri = Uri.fromFile(apkFile);
        request.setDestinationUri(destinationUri);

        DownloadManager downloadManager = (DownloadManager) getApplicationContext().getSystemService(Context.DOWNLOAD_SERVICE);
        if (downloadManager == null) {
            Log.e(TAG, "DownloadManager service not available.");
            return Result.failure();
        }

        try {
            // Remove existing file if it exists to prevent DownloadManager errors like "CANNOT_RESUME"
            // if a previous download attempt with the same file name failed or was interrupted.
            if (apkFile.exists()) {
                if (apkFile.delete()) {
                    Log.d(TAG, "Existing APK file deleted: " + apkFile.getAbsolutePath());
                } else {
                    Log.w(TAG, "Could not delete existing APK file: " + apkFile.getAbsolutePath());
                    // Depending on DownloadManager behavior, this might still work or might fail.
                }
            }

            long downloadId = downloadManager.enqueue(request);
            Log.d(TAG, "Download enqueued with ID: " + downloadId + " to " + apkFile.getAbsolutePath());

            // The worker's success here means the download was handed off to DownloadManager.
            // The actual installation trigger will need to observe DownloadManager completion separately or via notification.
            // We provide the absolute path of the file where the APK is being downloaded.
            // The UI/component that triggers installation will need to use FileProvider to generate a content URI.
            Data outputData = new Data.Builder()
                .putString(KEY_DOWNLOADED_APK_URI, apkFile.getAbsolutePath()) // Store absolute path
                .build();

            return Result.success(outputData);

        } catch (Exception e) {
            Log.e(TAG, "Error during download enqueue or file operation: " + e.getMessage(), e);
            return Result.failure(Data.error(e.getMessage())); // Pass error message in Data
        }
    }
}
