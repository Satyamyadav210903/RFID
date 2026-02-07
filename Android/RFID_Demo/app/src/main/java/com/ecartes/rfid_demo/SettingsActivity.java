package com.ecartes.rfid_demo;

import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.ecartes.rfid_demo.utils.SharedUtil;
import com.ecartes.rfid_demo.service.DataUploadService;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.materialswitch.MaterialSwitch;

public class SettingsActivity extends AppCompatActivity {

    private DataUploadService dataUploadService;
    private boolean isServiceBound = false;
    private SharedPreferences userPrefs;
    private SharedUtil sharedUtil;
    private int currentPower;
    private int currentFreqArea;
    private int currentSession;
    private int currentTarget;
    private int currentQValue;
    
    private com.google.android.material.textfield.TextInputEditText editPower;
    private com.google.android.material.textfield.TextInputEditText editQValue;
    private AutoCompleteTextView autoCompleteFreqArea;
    private AutoCompleteTextView autoCompleteSession;
    private AutoCompleteTextView autoCompleteTarget;
    private TextView textConnectionStatus;
    
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            DataUploadService.DataUploadBinder binder = (DataUploadService.DataUploadBinder) service;
            dataUploadService = binder.getService();
            isServiceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isServiceBound = false;
        }
    };
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settings);
        
        // Initialize shared utility and preferences
        sharedUtil = new SharedUtil(this);
        userPrefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        
        // Bind to DataUploadService
        Intent intent = new Intent(this, DataUploadService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);

        // Find views
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        MaterialSwitch switchUploadMode = findViewById(R.id.switchUploadMode);
        editPower = findViewById(R.id.editPower);
        editQValue = findViewById(R.id.editQValue);
        autoCompleteFreqArea = findViewById(R.id.autoCompleteFreqArea);
        autoCompleteSession = findViewById(R.id.autoCompleteSession);
        autoCompleteTarget = findViewById(R.id.autoCompleteTarget);
        android.widget.Button buttonApplySettings = findViewById(R.id.buttonApplySettings);
        textConnectionStatus = findViewById(R.id.textConnectionStatus);

        // Setup Frequency Area dropdown
        String[] areaNames = {"China1 (920-925)", "FCC (902-928)", "EU (865-867)"};
        final int[] areaValues = {0, 1, 2};
        ArrayAdapter<String> areaAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, areaNames);
        autoCompleteFreqArea.setAdapter(areaAdapter);

        // Setup Session dropdown
        String[] sessionNames = {"S0", "S1", "S2-Multi_tag", "S3"};
        ArrayAdapter<String> sessionAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, sessionNames);
        autoCompleteSession.setAdapter(sessionAdapter);

        // Setup Target dropdown
        String[] targetNames = {"A", "B", "A|B"};
        ArrayAdapter<String> targetAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, targetNames);
        autoCompleteTarget.setAdapter(targetAdapter);
        
        // Load saved settings
        loadPowerSettings();
        loadReaderStatus();
        
        // Update UI with current settings
        updatePowerDisplay();
        updateStatusDisplay();

        // Set up the toolbar
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        
        // Handle apply settings button click
        if(buttonApplySettings != null) {
            buttonApplySettings.setOnClickListener(v -> {
                handleApplySettings();
            });
        }
        
        // Handle upload mode switch
        switchUploadMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Save upload mode state to SharedPreferences
            userPrefs.edit().putBoolean("upload_mode_enabled", isChecked).apply();
            
            if (isChecked) {
                startDataUpload();
            } else {
                stopDataUploadService();
            }
        });
        
        // Load and set upload mode state
        boolean uploadModeEnabled = userPrefs.getBoolean("upload_mode_enabled", false);
        switchUploadMode.setChecked(uploadModeEnabled);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.settings), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
    
    private void startDataUpload() {
        String jwtToken = userPrefs.getString("jwt_token", null);
        if (jwtToken != null && !jwtToken.isEmpty()) {
            Intent uploadIntent = new Intent(this, DataUploadService.class);
            uploadIntent.putExtra("jwt_token", jwtToken);
            startService(uploadIntent);
            Toast.makeText(this, "Upload started in background", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Authentication required. Please login again.", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void stopDataUploadService() {
        Toast.makeText(this, "Upload service stopped", Toast.LENGTH_SHORT).show();
    }
    
    private void loadPowerSettings() {
        currentPower = sharedUtil.getPower();
        currentFreqArea = sharedUtil.getWorkFreq();
        currentSession = sharedUtil.getSession();
        currentTarget = sharedUtil.getTarget();
        currentQValue = sharedUtil.getQvalue();
    }
    
    private void savePowerSettings(int power, int freqArea, int session, int target, int qValue) {
        sharedUtil.savePower(power);
        sharedUtil.saveWorkFreq(freqArea);
        sharedUtil.saveSession(session);
        sharedUtil.saveTarget(target);
        sharedUtil.saveQvalue(qValue);
        
        currentPower = power;
        currentFreqArea = freqArea;
        currentSession = session;
        currentTarget = target;
        currentQValue = qValue;
        
        // Apply settings to actual reader
        try {
            com.handheld.uhfr.UHFRManager uhfrManager = com.handheld.uhfr.UHFRManager.getInstance();
            if (uhfrManager != null) {
                // Set Region/Area (0: China1, 1: FCC, 2: EU)
                com.uhf.api.cls.Reader.Region_Conf region;
                switch (freqArea) {
                    case 0: region = com.uhf.api.cls.Reader.Region_Conf.RG_PRC; break;
                    case 2: region = com.uhf.api.cls.Reader.Region_Conf.RG_EU3; break;
                    case 1: 
                    default: region = com.uhf.api.cls.Reader.Region_Conf.RG_NA; break;
                }
                uhfrManager.setRegion(region);
                
                // Set Power
                uhfrManager.setPower(power, power);

                // Set Params
                uhfrManager.setGen2session(session);
                uhfrManager.setTarget(target);
                uhfrManager.setQvaule(qValue);

                android.util.Log.d("SettingsActivity", "Applied settings to reader: Power=" + power + ", Area=" + freqArea + ", Session=" + session);
            }
        } catch (Exception e) {
            android.util.Log.e("SettingsActivity", "Failed to apply settings: " + e.getMessage());
        }
        
        // Update UI
        updatePowerDisplay();
    }
    
    private void updatePowerDisplay() {
        if (editPower != null) {
            editPower.setText(String.valueOf(currentPower));
        }
        if (editQValue != null) {
            editQValue.setText(String.valueOf(currentQValue));
        }
        if (autoCompleteFreqArea != null) {
            String[] areaNames = {"China1 (920-925)", "FCC (902-928)", "EU (865-867)"};
            if (currentFreqArea >= 0 && currentFreqArea < areaNames.length) {
                autoCompleteFreqArea.setText(areaNames[currentFreqArea], false);
            }
        }
        if (autoCompleteSession != null) {
            String[] sessionNames = {"S0", "S1", "S2-Multi_tag", "S3"};
            if (currentSession >= 0 && currentSession < sessionNames.length) {
                autoCompleteSession.setText(sessionNames[currentSession], false);
            }
        }
        if (autoCompleteTarget != null) {
            String[] targetNames = {"A", "B", "A|B"};
            if (currentTarget >= 0 && currentTarget < targetNames.length) {
                autoCompleteTarget.setText(targetNames[currentTarget], false);
            }
        }
    }

    private void handleApplySettings() {
        String powerStr = editPower.getText().toString().trim();
        String qValueStr = editQValue.getText().toString().trim();
        String areaStr = autoCompleteFreqArea.getText().toString().trim();
        String sessionStr = autoCompleteSession.getText().toString().trim();
        String targetStr = autoCompleteTarget.getText().toString().trim();
        
        if (powerStr.isEmpty()) {
            editPower.setError("Power is required");
            return;
        }
        
        if (qValueStr.isEmpty()) {
            editQValue.setError("Q Value is required");
            return;
        }
        
        try {
            int power = Integer.parseInt(powerStr);
            if (power < 0 || power > 33) {
                editPower.setError("Power must be 0-33 dBm");
                return;
            }
            int qValue = Integer.parseInt(qValueStr);
            if (qValue < 0 || qValue > 15) {
                editQValue.setError("Q Value must be 0-15");
                return;
            }
            
            // Map strings to values
            int freqArea = 1; // Default FCC
            String[] areaNames = {"China1 (920-925)", "FCC (902-928)", "EU (865-867)"};
            for (int i = 0; i < areaNames.length; i++) {
                if (areaNames[i].equals(areaStr)) { freqArea = i; break; }
            }

            int session = 0;
            String[] sessionNames = {"S0", "S1", "S2-Multi_tag", "S3"};
            for (int i = 0; i < sessionNames.length; i++) {
                if (sessionNames[i].equals(sessionStr)) { session = i; break; }
            }

            int target = 0;
            String[] targetNames = {"A", "B", "A|B"};
            for (int i = 0; i < targetNames.length; i++) {
                if (targetNames[i].equals(targetStr)) { target = i; break; }
            }
            
            // Save and apply settings
            savePowerSettings(power, freqArea, session, target, qValue);
            
            Toast.makeText(this, "Settings applied successfully", Toast.LENGTH_SHORT).show();
            
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid number format", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void updateStatusDisplay() {
        if (textConnectionStatus != null) {
            textConnectionStatus.setText("Checking...");
            textConnectionStatus.setTextColor(getResources().getColor(R.color.medium_grey, getTheme()));
        }
        
        // Run connection test in background to avoid blocking main thread (ANR prevention)
        new Thread(() -> {
            boolean isReaderConnected = testReaderConnection();
            
            runOnUiThread(() -> {
                String connectionText = isReaderConnected ? "Connected" : "Disconnected";
                int connectionColor = isReaderConnected ? R.color.success_green : R.color.warning_orange;
                
                if (textConnectionStatus != null) {
                    textConnectionStatus.setText(connectionText);
                    textConnectionStatus.setTextColor(getResources().getColor(connectionColor, getTheme()));
                }
            });
        }).start();
    }
    
    // Test actual communication with RFID reader (blocking call)
    private boolean testReaderConnection() {
        try {
            com.handheld.uhfr.UHFRManager uhfrManager = com.handheld.uhfr.UHFRManager.getInstance();
            if (uhfrManager != null) {
                // Try to get region as a connection test
                com.uhf.api.cls.Reader.Region_Conf region = uhfrManager.getRegion();
                return region != null;
            }
            return false;
        } catch (Exception e) {
            android.util.Log.e("SettingsActivity", "Reader connection test failed: " + e.getMessage());
            return false;
        }
    }
    
    private void loadReaderStatus() {
        // Currently using default values
    }
    
    private void saveReaderStatus() {
        // Save status settings here
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isServiceBound) {
            unbindService(serviceConnection);
            isServiceBound = false;
        }
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // Handle back button press
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}