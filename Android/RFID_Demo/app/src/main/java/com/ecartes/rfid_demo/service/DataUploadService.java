package com.ecartes.rfid_demo.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.ecartes.rfid_demo.api.ApiClient;
import com.ecartes.rfid_demo.api.ApiService;
import com.ecartes.rfid_demo.data.DatabaseHelper;
import com.ecartes.rfid_demo.model.ScanLog;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DataUploadService extends Service {

    private static final String TAG = "DataUploadService";

    private final IBinder binder = new DataUploadBinder();
    private ApiService apiService;
    private DatabaseHelper databaseHelper;

    public class DataUploadBinder extends Binder {
        public DataUploadService getService() {
            return DataUploadService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        apiService = ApiClient.getApiService();
        databaseHelper = new DatabaseHelper(this);
        Log.d(TAG, "Service created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        String jwtToken = intent.getStringExtra("jwt_token");
        if (jwtToken != null) {
            uploadScanLogs(jwtToken, (success, message) -> {
                Log.d(TAG, "Upload finished: " + message);

                Intent broadcast = new Intent("UPLOAD_COMPLETED");
                broadcast.putExtra("success", success);
                broadcast.putExtra("message", message);
                sendBroadcast(broadcast);
            });
        }

        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    // ===================== SCAN LOG UPLOAD ONLY =====================

    private void uploadScanLogs(String jwtToken, UploadCallback callback) {

        String authHeader = "Bearer " + jwtToken;

        List<ScanLog> scanLogs = databaseHelper.getLocalScanLogsForUpload();

        if (scanLogs.isEmpty()) {
            callback.onComplete(true, "No scan logs to upload");
            return;
        }

        AtomicInteger pending = new AtomicInteger(scanLogs.size());

        Runnable checkDone = () -> {
            if (pending.decrementAndGet() == 0) {
                callback.onComplete(true, "All scan logs uploaded");
            }
        };

        for (ScanLog scanLog : scanLogs) {
            apiService.createScanLog(authHeader, scanLog)
                    .enqueue(new Callback<ScanLog>() {

                        @Override
                        public void onResponse(Call<ScanLog> call,
                                               Response<ScanLog> response) {

                            if (response.isSuccessful() && response.body() != null) {
                                databaseHelper.markScanLogAsUploaded(scanLog.getLogId());
                                Log.d(TAG, "ScanLog uploaded: " + scanLog.getLogId());
                            } else {
                                Log.e(TAG, "ScanLog upload failed. HTTP " + response.code());
                            }
                            checkDone.run();
                        }

                        @Override
                        public void onFailure(Call<ScanLog> call, Throwable t) {
                            Log.e(TAG, "ScanLog upload network error", t);
                            checkDone.run();
                        }
                    });
        }
    }

    // ================================================================

    interface UploadCallback {
        void onComplete(boolean success, String message);
    }
}