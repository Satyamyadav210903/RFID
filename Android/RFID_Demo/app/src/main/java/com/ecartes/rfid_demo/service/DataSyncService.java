package com.ecartes.rfid_demo.service;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.ecartes.rfid_demo.api.ApiClient;
import com.ecartes.rfid_demo.api.ApiService;
import com.ecartes.rfid_demo.data.DatabaseHelper;
import com.ecartes.rfid_demo.model.ApiResponse;
import com.ecartes.rfid_demo.model.Product;
import com.ecartes.rfid_demo.model.ScanLog;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DataSyncService extends Service {
    
    private static final String TAG = "DataSyncService";
    private final IBinder binder = new DataSyncBinder();
    private ApiService apiService;
    private DatabaseHelper databaseHelper;
    
    public class DataSyncBinder extends Binder {
        public DataSyncService getService() {
            return DataSyncService.this;
        }
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        apiService = ApiClient.getApiService();
        databaseHelper = new DatabaseHelper(this);
        Log.d(TAG, "DataSyncService created");
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String jwtToken = intent.getStringExtra("jwt_token");
        if (jwtToken != null) {
            syncData(jwtToken, new DataSyncCallback() {
                @Override
                public void onSyncComplete(boolean success, String message) {
                    Log.d(TAG, "Sync completed: " + message);
                    // Send broadcast to notify about sync completion
                    Intent broadcastIntent = new Intent("SYNC_COMPLETED");
                    broadcastIntent.putExtra("success", success);
                    broadcastIntent.putExtra("message", message);
                    sendBroadcast(broadcastIntent);
                }
            });
        }
        return START_NOT_STICKY; // Service won't restart if killed
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "DataSyncService bound");
        return binder;
    }
    
    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "DataSyncService unbound");
        return super.onUnbind(intent);
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "DataSyncService destroyed");
    }
    
    public void syncData(String jwtToken, DataSyncCallback callback) {
        Log.d(TAG, "Starting data sync...");
        
        // Fetch products directly as array
        Call<List<Product>> productsCall = apiService.getAllProducts(jwtToken);
        productsCall.enqueue(new Callback<List<Product>>() {
            @Override
            public void onResponse(Call<List<Product>> call, Response<List<Product>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Product> products = response.body();
                    // Insert products into local DB
                    databaseHelper.insertProductsFromApi(products);
                    
                    // Now fetch scan logs
                    fetchAndSyncScanLogs(jwtToken, callback);
                } else {
                    callback.onSyncComplete(false, "Failed to fetch products: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<List<Product>> call, Throwable t) {
                callback.onSyncComplete(false, "Network error (products): " + t.getMessage());
            }
        });
    }
    
    private void fetchAndSyncScanLogs(String jwtToken, DataSyncCallback callback) {
        // Fetch scan logs directly as array
        Call<List<ScanLog>> scanLogsCall = apiService.getAllScanLogs(jwtToken);
        scanLogsCall.enqueue(new Callback<List<ScanLog>>() {
            @Override
            public void onResponse(Call<List<ScanLog>> call, Response<List<ScanLog>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<ScanLog> scanLogs = response.body();
                    // Insert scan logs into local DB
                    databaseHelper.insertScanLogsFromApi(scanLogs);
                    
                    // Sync completed successfully
                    callback.onSyncComplete(true, "Sync completed successfully!");
                } else {
                    callback.onSyncComplete(false, "Failed to fetch scan logs: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<List<ScanLog>> call, Throwable t) {
                callback.onSyncComplete(false, "Network error (scan logs): " + t.getMessage());
            }
        });
    }
    
    public interface DataSyncCallback {
        void onSyncComplete(boolean success, String message);
    }
}