//
package com.omerflex.service;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.omerflex.entity.ServerConfig;
import com.omerflex.entity.dto.ServerConfigDTO;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class UpdateService {

    private static final String TAG = "UpdateActivity";

    private static final int REQUEST_CODE_PERMISSIONS = 100;
    private static final int REQUEST_CODE_INSTALL_PERMISSION = 100;
    private static final int REQUEST_CODE_STORAGE_PERMISSION = 101;
    private boolean permissionRequested = false;
    private static String APK_URL = "https://github.com/alyabroudy1/omerFlex_3/raw/refs/heads/mobile/app/omerFlex.apk";
    private static String APK_NAME = "omerFlex";

    private long downloadId;
    private BroadcastReceiver downloadCompleteReceiver;
    Fragment fragment;
    Activity activity;

    public UpdateService(Fragment fragment) {
        this.fragment = fragment;
        this.activity = fragment.getActivity();
    }

    public void checkForUpdates(ServerConfigDTO githubServerConfigDTO) {
        String version = githubServerConfigDTO.description;
        try {
            int number = Integer.parseInt(version);
            // Use the number
            Log.d(TAG, "checkForUpdates: " + version + ", n: " + number);
            if (toBeUpdated(number)) {
                showUpdateDialog(githubServerConfigDTO.url);
            }
        } catch (NumberFormatException e) {
            // Handle the case where the string is not a valid integer
            Log.d(TAG, "checkForUpdates: fail reading int version number: " + version);
        }

    }

    public boolean toBeUpdated(int newVersionCode) {
        // Get the package manager
        PackageManager pm = activity.getPackageManager();
// Get the package info
        PackageInfo packageInfo = null;
        try {
            packageInfo = pm.getPackageInfo(activity.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException(e);
        }
// Get the current version code
        int currentVersion = packageInfo.versionCode;
        APK_NAME = APK_NAME + "_v"+ newVersionCode;
// Get the current version name
        String currentVersionName = packageInfo.versionName;

//        Log.d(TAG, "toBeUpdated: version: " + currentVersion + ", name: " + currentVersionName);
//        Log.d(TAG, "toBeUpdated: new version: " + newVersionCode);
//        Log.d(TAG, "toBeUpdated: new APK_NAME: " + APK_NAME);
        return newVersionCode > currentVersion ;
//        return true;
    }

    private void showUpdateDialog(String url) {
        new AlertDialog.Builder(activity)
                .setTitle("تحديث")
                .setMessage("إصدار جديد من التطبيق متاح. هل تريد التحديث الآن؟")
                .setPositiveButton("تحديث", (dialog, which) -> startDownload(url))
                .setNegativeButton("إلغاء", (dialog, which) -> Toast.makeText(activity, "إلغاء التحديث...", Toast.LENGTH_SHORT).show())
                .setCancelable(false)
                .show();
    }

    private void startDownload(String url) {
        // Create a download request
        Log.d(TAG, "startDownload: ");
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setTitle("App Update");
        request.setDescription("Downloading the latest version...");
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, APK_NAME+".apk");

        // Enqueue the download
        DownloadManager downloadManager = (DownloadManager) activity.getSystemService(Context.DOWNLOAD_SERVICE);
        if (downloadManager != null) {
            downloadId = downloadManager.enqueue(request);
            Log.d(TAG, "startDownload: downloadId: " + downloadId);
        } else {
            Toast.makeText(activity, "DownloadManager is not available.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Register a BroadcastReceiver to listen for download completion
        downloadCompleteReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                Log.d(TAG, "onReceive: " + id);
                if (id == downloadId) {
                    installUpdate();
                }
            }
        };
        // Register the receiver with the appropriate flag for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            activity.registerReceiver(
                    downloadCompleteReceiver,
                    new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                    Context.RECEIVER_EXPORTED
            );
        } else {
            activity.registerReceiver(
                    downloadCompleteReceiver,
                    new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
            );
        }
    }

    private void installUpdate() {
        Uri apkUri = getDownloadedApkUri();
        if (apkUri == null) return; // Exit if no files match the criteria
        Log.d(TAG, "installUpdate: uri: " + apkUri.toString());

        // Check if the app has permission to install packages
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!activity.getPackageManager().canRequestPackageInstalls()) {
                // Request permission to install packages
                Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
                intent.setData(Uri.parse("package:" + activity.getPackageName()));
                // note: the fragment not the activity
                fragment.startActivityForResult(intent, REQUEST_CODE_INSTALL_PERMISSION);
                return;
            }
        }

        // If permission is already granted, proceed with installation
        proceedWithInstallation(apkUri);
    }

    @Nullable
    private Uri getDownloadedApkUri() {
        // Get the download directory
        File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        // Filter files to only those with the name pattern
        File[] apkFiles = downloadDir.listFiles((dir, name) -> name.startsWith(APK_NAME) && name.endsWith(".apk"));

        if (apkFiles == null || apkFiles.length == 0) {
            Toast.makeText(activity, "APK file not found.", Toast.LENGTH_SHORT).show();
            return null;
        }

        // Find the most recent file
        File latestApkFile = Arrays.stream(apkFiles)
                .max(Comparator.comparingLong(File::lastModified))
                .orElse(null);

        if (latestApkFile == null) {
            Toast.makeText(activity, "No valid APK file found.", Toast.LENGTH_SHORT).show();
            return null;
        }

        Log.d(TAG, "installUpdate: Latest APK file: " + latestApkFile.getName());

        // Create a content URI using FileProvider for the latest APK file
        Uri apkUri = FileProvider.getUriForFile(activity, activity.getApplicationContext().getPackageName() + ".provider", latestApkFile);
        return apkUri;
    }

    private void proceedWithInstallation(Uri apkUri) {
        // Start the installation
        Intent installIntent = new Intent(Intent.ACTION_VIEW);
        installIntent.setDataAndType(apkUri, "application/vnd.android.package-archive");
        installIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        activity.startActivity(installIntent);
//    dbHelper.saveServerConfig(ServerConfigRepository.getConfig(Movie.SERVER_APP));
    }

    public void handleOnActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult: " + requestCode);
        if (requestCode != REQUEST_CODE_INSTALL_PERMISSION) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && activity.getPackageManager().canRequestPackageInstalls()) {
            Log.d(TAG, "onActivityResult: installUpdate");
            Uri apkUri = getDownloadedApkUri();
            proceedWithInstallation(apkUri);
        } else {
            Toast.makeText(activity, "Install permission denied.", Toast.LENGTH_SHORT).show();
        }

    }

    public void handleOnDestroy() {
        if (downloadCompleteReceiver != null) {
            activity.unregisterReceiver(downloadCompleteReceiver); // Unregister the receiver
        }
    }

    ///------------

    private void checkAndRequestPermissions() {
        // List of permissions to request
        List<String> permissionsToRequest = new ArrayList<>();

        // Check and add permissions based on Android version
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ requires media-specific permissions
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_IMAGES);
            }
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_VIDEO);
            }
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_AUDIO);
            }
        } else {
            // Android 12 and below require legacy storage permissions
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
        }

        // Request permissions if needed
        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(activity, permissionsToRequest.toArray(new String[0]), REQUEST_CODE_PERMISSIONS);
        } else {
            // All permissions are already granted
            Toast.makeText(activity, "All permissions granted!", Toast.LENGTH_SHORT).show();
        }
    }

    public void handleOnRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode != REQUEST_CODE_PERMISSIONS) {
            return;
        }
        boolean allPermissionsGranted = true;
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                allPermissionsGranted = false;
                break;
            }
        }

        if (allPermissionsGranted) {
            Toast.makeText(activity, "All permissions granted!", Toast.LENGTH_SHORT).show();
        } else {
            // Show a dialog explaining why permissions are needed
            showPermissionExplanationDialog();
        }

    }

    private void showPermissionExplanationDialog() {
        new AlertDialog.Builder(activity)
                .setTitle("Permissions Required")
                .setMessage("This app needs permissions to access storage to function properly. Please grant the permissions in the app settings.")
                .setPositiveButton("Go to Settings", (dialog, which) -> openAppSettings())
                .setNegativeButton("Cancel", (dialog, which) -> Toast.makeText(activity, "Permissions denied. Some features may not work.", Toast.LENGTH_SHORT).show())
                .show();
    }

    private void openAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.fromParts("package", activity.getPackageName(), null));
        activity.startActivity(intent);
    }

}
//
//    private boolean hasStoragePermission() {
//        boolean writePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
//        boolean readPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
//        boolean managePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.MANAGE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
//        boolean mediaPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED;
//
//        Log.d("TAG", "Write Permission: " + writePermission);
//        Log.d("TAG", "Read Permission: " + readPermission);
//        Log.d("TAG", "MEdia Permission: " + mediaPermission);
//
//
//        // Check if the permissions are granted
////        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
////                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
////
////            // Request the permissions
////            ActivityCompat.requestPermissions(this,
////                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
////                    REQUEST_CODE_STORAGE_PERMISSION);
////        }
//
//        return writePermission && readPermission && managePermission;
//    }
//
////    private boolean hasStoragePermission() {
////        return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
////                && ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
////    }
//
//    private void requestStoragePermission() {
//        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.MANAGE_EXTERNAL_STORAGE, Manifest.permission.READ_MEDIA_IMAGES,
//                Manifest.permission.READ_MREAD_MEDIA_VIDEO,
//                Manifest.permission.READ_MEDIA_AUDIO}, REQUEST_CODE_STORAGE_PERMISSION);
//    }
//
//    @Override
//    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//        if (requestCode == REQUEST_CODE_STORAGE_PERMISSION) {
//            Log.d(TAG, "onRequestPermissionsResult: "+REQUEST_CODE_STORAGE_PERMISSION);
//            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                downloadUpdate();
//            } else {
//                Toast.makeText(this, "Storage permission denied.", Toast.LENGTH_SHORT).show();
//            }
//        }
//    }
//
//    private void requestMediaPermissions() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//            ActivityCompat.requestPermissions(
//                    this,
//                    new String[]{
//                            Manifest.permission.READ_MEDIA_IMAGES,
//                            Manifest.permission.READ_MEDIA_VIDEO,
//                            Manifest.permission.READ_MEDIA_AUDIO
//                    },
//                    REQUEST_CODE_STORAGE_PERMISSION
//            );
//        }
//    }
//
//
//    private void downloadUpdate() {
//        Log.d(TAG, "downloadUpdate: ");
//        String apkUrl = "https://github.com/alyabroudy1/omerFlex_3/raw/refs/heads/main/app/omerFlex%2001%20juni%202024.apk";
//
//        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(apkUrl));
//        request.setMimeType("application/vnd.android.package-archive");
//        request.setDescription("Downloading the new update...");
//        request.setTitle("Your App Update");
//        request.allowScanningByMediaScanner();
//        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
//
//        File destinationDir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
//        File apkFile = new File(destinationDir, "yourapp.apk");
//        try {
//        Uri apkUri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".provider", apkFile);
//        request.setDestinationUri(apkUri);
//        DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
//
//        long downloadId = downloadManager.enqueue(request);
//        }catch (Exception e){
//            Log.d(TAG, "downloadUpdate: error: "+ e.getMessage());
//        }
//
//
//        downloadCompleteReceiver = new BroadcastReceiver() {
//            @Override
//            public void onReceive(Context context, Intent intent) {
//                long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
//                DownloadManager.Query query = new DownloadManager.Query();
//                query.setFilterById(downloadId);
//
//                DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
//                Cursor cursor = downloadManager.query(query);
//                if (cursor.moveToFirst()) {
//                    int status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
//                    if (status == DownloadManager.STATUS_SUCCESSFUL) {
//                        String localUri = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
//                        Uri newApkUri = Uri.parse(localUri);
//                        installUpdate(newApkUri);
//                    } else if (status == DownloadManager.STATUS_FAILED) {
//                        Toast.makeText(context, "Download failed.", Toast.LENGTH_SHORT).show();
//                    }
//                }
//                cursor.close();
//                unregisterReceiver(this);
//            }
//        };
//
//        // Register BroadcastReceiver to listen for download completion
//        IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
//        registerReceiver(downloadCompleteReceiver, filter);
//    }
//
////    private void downloadUpdate() {
////        // Check available storage space
////
////        StatFs stat = new StatFs(Environment.getExternalStorageDirectory().getPath());
////        long bytesAvailable = stat.getBlockSizeLong() * stat.getAvailableBlocksLong();
////        long minRequiredSpace = 100 * 1024 * 1024; // 100 MB, adjust as needed
////        if (bytesAvailable < minRequiredSpace) {
////            Toast.makeText(this, "Not enough storage space.", Toast.LENGTH_SHORT).show();
////            return;
////        }
////        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(apkUrl));
////        request.setMimeType("application/vnd.android.package-archive");
////        request.setDescription("Downloading the new update...");
////        request.setTitle("Your App Update");
////        request.allowScanningByMediaScanner();
////        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
////        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "yourapp.apk");
////
////        DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
////        downloadManager.enqueue(request);
////
////        // Register receiver to listen for download completion
////        downloadCompleteReceiver = new BroadcastReceiver() {
////            @Override
////            public void onReceive(Context context, Intent intent) {
////                Log.d(TAG, "onReceive: ");
////                long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
////                DownloadManager.Query query = new DownloadManager.Query();
////                query.setFilterById(downloadId);
////                DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
////                Cursor cursor = downloadManager.query(query);
////                if (cursor.moveToFirst()) {
////                    Log.d(TAG, "onReceive: moveToFirst");
////                    int status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
////                    if (status == DownloadManager.STATUS_SUCCESSFUL) {
////                        installUpdate();
////                    } else if (status == DownloadManager.STATUS_FAILED) {
////                        Toast.makeText(context, "Download failed.", Toast.LENGTH_SHORT).show();
////                    }
////                }
////                cursor.close();
////                unregisterReceiver(this);
////            }
////        };
////        Log.d(TAG, "downloadUpdate: Build: " + Build.VERSION.SDK_INT);
////
////        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
////            Log.d(TAG, "downloadUpdate: Build: >= Build.VERSION_CODES.O");
////            registerReceiver(downloadCompleteReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_EXPORTED);
////        } else {
////            Log.d(TAG, "downloadUpdate: Build: else");
////            registerReceiver(downloadCompleteReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
////        }
////    }
//
//
//
//
//    public boolean toBeUpdated(){
//        // Get the package manager
//        PackageManager pm = getPackageManager();
//// Get the package info
//        PackageInfo packageInfo = null;
//        try {
//            packageInfo = pm.getPackageInfo(getPackageName(), 0);
//        } catch (PackageManager.NameNotFoundException e) {
//            throw new RuntimeException(e);
//        }
//// Get the current version code
//        int currentVersion = packageInfo.versionCode;
//// Get the current version name
//        String currentVersionName = packageInfo.versionName;
//
//        return true;
//    }
//
//    private void showUpdateDialog() {
//        new AlertDialog.Builder(this)
//                .setTitle("Update Available")
//                .setMessage("A new version is available. Do you want to update?")
//                .setPositiveButton("Update", new DialogInterface.OnClickListener() {
//                    public void onClick(DialogInterface dialog, int which) {
//                        if (hasStoragePermission()) {
//                            downloadUpdate();
//                        } else {
//                            requestStoragePermission();
//                        }
//                    }
//                })
//                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
//                    public void onClick(DialogInterface dialog, int which) {
//                        // Do nothing
//                    }
//                })
//                .show();
//    }
//
////    private void installUpdate() {
////        Log.d(TAG, "installUpdate: ");
////        File apkFile = new File(Environment.getExternalStorageDirectory(), "Downloads/yourapp.apk");
////        Log.d(TAG, "installUpdate: apkFile");
////
////        if (apkFile.exists()) {
////            if (isInstallAllowed()) {
////                Intent intent = new Intent(Intent.ACTION_VIEW);
////                intent.setDataAndType(Uri.fromFile(apkFile), "application/vnd.android.package-archive");
////                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
////                startActivity(intent);
////            } else {
////                requestInstallPermission();
////            }
////        } else {
////            Toast.makeText(this, "APK file not found.", Toast.LENGTH_SHORT).show();
////        }
////    }
//
//    private void installUpdate(Uri apkUri) {
//        Intent intent = new Intent(Intent.ACTION_VIEW);
//        intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
//        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
//        startActivity(intent);
//    }
//    private boolean isInstallAllowed() {
//        Log.d(TAG, "isInstallAllowed: ");
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            return getPackageManager().canRequestPackageInstalls();
//        } else {
//            return true;
//        }
//    }
//
//    private void requestInstallPermission() {
//        Log.d(TAG, "requestInstallPermission: ");
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
//            intent.setData(Uri.parse("package:" + getPackageName()));
//            startActivityForResult(intent, 100);
//        }
//    }
//
//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        Log.d(TAG, "onActivityResult: ");
//        super.onActivityResult(requestCode, resultCode, data);
//        if (requestCode == 100) {
//            if (isInstallAllowed()) {
////                installUpdate();
//            } else {
//                Toast.makeText(this, "Installation permission denied.", Toast.LENGTH_SHORT).show();
//            }
//        }
//    }
//
//    @Override
//    protected void onDestroy() {
//        Log.d(TAG, "onDestroy: ");
//        super.onDestroy();
//        if (downloadCompleteReceiver != null) {
//            unregisterReceiver(downloadCompleteReceiver);
//        }
//    }
//
//    private void checkForUpdates() {
//        // Simulate checking for updates
//        // In a real scenario, you would check an API or download a version file
//        // For this example, we'll assume an update is available
//        showUpdateDialog();
//    }
//}