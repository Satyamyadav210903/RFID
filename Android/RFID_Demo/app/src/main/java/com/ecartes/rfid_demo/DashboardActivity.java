package com.ecartes.rfid_demo;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.ecartes.rfid_demo.api.ApiClient;
import com.ecartes.rfid_demo.api.ApiService;
import com.ecartes.rfid_demo.data.DatabaseHelper;
import com.ecartes.rfid_demo.model.ApiResponse;
import com.ecartes.rfid_demo.model.User;
import com.ecartes.rfid_demo.model.Product;
import com.ecartes.rfid_demo.model.ScanLog;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.materialswitch.MaterialSwitch;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import com.handheld.uhfr.UHFRManager;
import com.uhf.api.cls.Reader;
import com.gg.reader.api.utils.XhPower;
import android.util.Log;

public class DashboardActivity extends AppCompatActivity {

    private ApiService apiService;
    private DatabaseHelper dbHelper;
    private SharedPreferences sharedPreferences;

    // Counter TextView references
    private TextView textTotalItems;
    private TextView textScannedToday;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_dashboard);

        // Initialize API service, database helper and shared preferences
        apiService = ApiClient.getApiService();
        dbHelper = new DatabaseHelper(this);
        sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);

        // Find views
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        View cardScan = findViewById(R.id.cardScan);
        View cardViewInventory = findViewById(R.id.cardViewInventory);
        View cardWriteTag = findViewById(R.id.cardWriteTag);
        View cardSyncData = findViewById(R.id.cardSyncData);
        View cardLogout = findViewById(R.id.cardLogout);

        // Find counter TextViews
        textTotalItems = findViewById(R.id.textTotalItems);
        textScannedToday = findViewById(R.id.textScannedToday);

        // Set up the toolbar
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            // Remove back button
        }

        // Handle scan card click
        if(cardScan != null) {
            cardScan.setOnClickListener(v -> {
                Intent intent = new Intent(DashboardActivity.this, RFIDScanActivity.class);
                startActivity(intent);
            });
        }

        // Handle view inventory card click
        if(cardViewInventory != null) {
            cardViewInventory.setOnClickListener(v -> {
                Intent intent = new Intent(DashboardActivity.this, InventoryListActivity.class);
                startActivity(intent);
            });
        }



        // Handle sync data card click
        if(cardSyncData != null) {
            cardSyncData.setOnClickListener(v -> {
                showSyncDataDialog();
            });
        }
        
        // Handle write tag card click
        if(cardWriteTag != null) {
            cardWriteTag.setOnClickListener(v -> {
                Intent intent = new Intent(DashboardActivity.this, RFIDWriteActivity.class);
                startActivity(intent);
            });
        }

        // Handle logout card click
        if(cardLogout != null) {
            cardLogout.setOnClickListener(v -> {
                // Clear all session data
                getSharedPreferences("UserPrefs", MODE_PRIVATE)
                        .edit()
                        .clear()
                        .apply();

                Toast.makeText(DashboardActivity.this, "Logged out successfully", Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(DashboardActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        }



        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.dashboard), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Update counters when activity loads
        updateCounters();

        // Initialize RFID reader at startup
        initRFID();
    }

    private void initRFID() {
        new Thread(() -> {
            try {
                Log.d("DashboardActivity", "Starting RFID initialization...");
                // 1. Power on UHF module
                XhPower.setPower(true);
                Thread.sleep(500); // Stabilization delay

                // 2. Initialize UHFRManager
                UHFRManager uhfr = UHFRManager.getInstance();
                if (uhfr != null) {
                    Log.d("DashboardActivity", "RFID Reader initialized successfully");
                    runOnUiThread(() -> {
                        Toast.makeText(DashboardActivity.this, "RFID Reader Connected", Toast.LENGTH_SHORT).show();
                    });
                } else {
                    Log.e("DashboardActivity", "Failed to get UHFRManager instance");
                    runOnUiThread(() -> {
                        Toast.makeText(DashboardActivity.this, "RFID Reader Not Connected", Toast.LENGTH_LONG).show();
                    });
                }
            } catch (Exception e) {
                Log.e("DashboardActivity", "RFID Init Error: " + e.getMessage());
                runOnUiThread(() -> {
                    Toast.makeText(DashboardActivity.this, "Reader Init Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Update counters when returning to dashboard
        updateCounters();
    }

    private void updateCounters() {
        if (textTotalItems != null) {
            int totalCount = dbHelper.getTotalScanLogCount();
            textTotalItems.setText(String.valueOf(totalCount));
        }

        if (textScannedToday != null) {
            int todayCount = dbHelper.getTodayScanLogCount();
            textScannedToday.setText(String.valueOf(todayCount));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.dashboard_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            // Open Settings Activity
            Intent intent = new Intent(DashboardActivity.this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showSyncDataDialog() {
        Dialog dialog = new Dialog(this, R.style.Theme_RFID_Demo_Dialog);
        dialog.setContentView(R.layout.dialog_sync_data);

        // Find dialog views
        android.widget.ProgressBar progressBar = dialog.findViewById(R.id.progressBar);
        android.widget.TextView textStatus = dialog.findViewById(R.id.textStatus);
        android.widget.Button buttonCancel = dialog.findViewById(R.id.buttonCancel);
        android.widget.Button buttonClose = dialog.findViewById(R.id.buttonClose);

        // Initially hide close button and show cancel
        buttonClose.setVisibility(android.widget.Button.GONE);
        buttonCancel.setVisibility(android.widget.Button.VISIBLE);

        // Get JWT token from shared preferences
        String jwtToken = sharedPreferences.getString("jwt_token", null);

        if (jwtToken == null || jwtToken.isEmpty()) {
            textStatus.setText("Error: Not authenticated. Please login again.");
            buttonCancel.setVisibility(android.widget.Button.GONE);
            buttonClose.setVisibility(android.widget.Button.VISIBLE);
            return;
        }

        // Set up broadcast receiver to listen for sync completion
        android.content.BroadcastReceiver syncReceiver = new android.content.BroadcastReceiver() {
            @Override
            public void onReceive(android.content.Context context, android.content.Intent intent) {
                if ("SYNC_COMPLETED".equals(intent.getAction())) {
                    boolean success = intent.getBooleanExtra("success", false);
                    String message = intent.getStringExtra("message");

                    runOnUiThread(() -> {
                        if (success) {
                            progressBar.setProgress(100);
                            textStatus.setText(message);
                            // Update counters after successful sync
                            updateCounters();
                        } else {
                            textStatus.setText("Sync failed: " + message);
                        }

                        // Update buttons
                        buttonCancel.setVisibility(android.widget.Button.GONE);
                        buttonClose.setVisibility(android.widget.Button.VISIBLE);
                    });
                }
            }
        };

        // Register the receiver
        IntentFilter filter = new IntentFilter("SYNC_COMPLETED");
        androidx.core.content.ContextCompat.registerReceiver(this, syncReceiver, filter, androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED);

        // Dismiss dialog when close button is clicked
        buttonClose.setOnClickListener(v -> {
            try {
                unregisterReceiver(syncReceiver);
            } catch (Exception e) {
                // Receiver might already be unregistered
            }
            dialog.dismiss();
        });

        // Cancel sync when cancel button is clicked
        buttonCancel.setOnClickListener(v -> {
            try {
                unregisterReceiver(syncReceiver);
            } catch (Exception e) {
                // Receiver might already be unregistered
            }
            dialog.dismiss();
        });

        // Start the sync service
        Intent syncIntent = new Intent(this, com.ecartes.rfid_demo.service.DataSyncService.class);
        syncIntent.putExtra("jwt_token", "Bearer " + jwtToken);  // Add Bearer prefix
        startService(syncIntent);

        // Show initial status
        textStatus.setText("Sync started...");
        progressBar.setProgress(10);

        dialog.show();
    }



}