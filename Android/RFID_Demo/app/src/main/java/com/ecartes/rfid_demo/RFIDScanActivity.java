package com.ecartes.rfid_demo;

import static com.gg.reader.api.utils.XhPower.setPower;

import com.gg.reader.api.utils.XhPower;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.PowerManager;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.ecartes.rfid_demo.common.Common;
import com.google.android.material.textfield.TextInputEditText;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ecartes.rfid_demo.model.RfidTag;
import com.ecartes.rfid_demo.model.ScanLog;
import com.uhf.api.cls.Reader;
import com.handheld.uhfr.UHFRManager;
import com.ecartes.rfid_demo.utils.SharedUtil;
import com.ecartes.rfid_demo.utils.Tools;
import com.ecartes.rfid_demo.utils.UtilSound;
import com.ecartes.rfid_demo.common.Common;
import com.google.android.material.appbar.MaterialToolbar;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

public class RFIDScanActivity extends AppCompatActivity {

    private int tagCount = 0;
    private int tagLimit = 10; // Default limit
    private List<RfidTag> scannedTags;
    private java.util.Map<String, RfidTag> tagInfoMap = new java.util.LinkedHashMap<>();
    private RfidTagAdapter adapter;
    private RecyclerView recyclerViewTags;
    private TextView textScannedTagsLabel;
    private TextView textScanStatus;
    private TextView textScanMessage;

    private SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
    private Timer timer ;
    private long speed = 0 ;
    // RFID Manager
    private UHFRManager mUhfrManager;
    private final String TAG = RFIDScanActivity.class.getSimpleName();
    private Activity activity;
    
    // Power management
    private PowerManager.WakeLock wakeLock;

    // Continuous scanning
    private boolean isScanning = false;
    private Thread scanningThread;
    private Button buttonScan;





    private boolean f1hidden = false;
    private volatile boolean mIsLoop = false;
    String noSpacesEpc = "";
    private boolean isPlay = true;// multi mode flag

    boolean isSuccess = false;
    boolean isConnected;
    private boolean isStart = false;
    int count = 0;
    private boolean isMulti = false;
    private boolean isTid = false;
    Boolean rfidIsActive = false;
    private SharedUtil sharedUtil;
    private com.ecartes.rfid_demo.data.DatabaseHelper databaseHelper;

    private final int MSG_INVENROTY = 1 ;

    private final int MSG_INVENROTY_TIME = 1001 ;

    private long lastCount = 0 ;

    private int time = 0;
    private Handler handler = new Handler(Looper.getMainLooper()){
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);

            switch (msg.what) {
                case MSG_INVENROTY:
                    // Update tag list and UI components
                    scannedTags.clear();
                    scannedTags.addAll(tagInfoMap.values());
                    adapter.notifyDataSetChanged();
                    tagCount = scannedTags.size();
                    textScannedTagsLabel.setText("SCANNED TAGS (" + tagCount + ")");
                    if (!scannedTags.isEmpty()) {
                        RfidTag lastTag = scannedTags.get(scannedTags.size() - 1);
                        textScanMessage.setText("Last scanned: " + lastTag.getTagId());
                    }
                    break;

                case MSG_INVENROTY_TIME:
                    time++;
                    // Optional: Update time display if needed
                    break;
            }
        }
    };



    // Key receiver for hardware button
    private BroadcastReceiver keyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int keyCode = intent.getIntExtra("keyCode", 0);
            if (keyCode == 0) {
                keyCode = intent.getIntExtra("keycode", 0);
            }
            boolean keyDown = intent.getBooleanExtra("keydown", false);
            
            if (!keyDown) {
                android.util.Log.d(TAG, "Hardware Key Up: " + keyCode);
                switch (keyCode) {
                    case KeyEvent.KEYCODE_F1:
                    case KeyEvent.KEYCODE_F2:
                    case KeyEvent.KEYCODE_F3: // C510x
                    case KeyEvent.KEYCODE_F4: // 6100
                    case KeyEvent.KEYCODE_F5:
                    case KeyEvent.KEYCODE_F7: // H3100
                    case 280: case 281: case 282: case 283: // Trigger keys
                    case 523: case 524: // Specific PDA codes
                        handleHardwareButtonPress();
                        break;
                }
            }
        }
    };
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_rfid_scan);

        // Initialize data
        scannedTags = new ArrayList<>();
        activity = this;
        sharedUtil = new SharedUtil(this);
        
        // Initialize power management
        initPowerManagement();
        
        // Initialize sound
        UtilSound.initSoundPool(this);
        
        
        // Find views
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        buttonScan = findViewById(R.id.buttonScan);
        Button buttonSave = findViewById(R.id.buttonSave);
        Button buttonDiscard = findViewById(R.id.buttonDiscard);
        textScanStatus = findViewById(R.id.textScanStatus);
        textScanMessage = findViewById(R.id.textScanMessage);
        Button buttonSetLimit = findViewById(R.id.buttonSetLimit);
        textScannedTagsLabel = findViewById(R.id.textScannedTagsLabel);

        recyclerViewTags = findViewById(R.id.recyclerViewTags);

        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();

        final Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isConnected()) {
                    // Applied power settings
                }
            }
        }, 100);

        // Initialize RFID reader
        initModule();
        
        // Initialize database helper
        databaseHelper = new com.ecartes.rfid_demo.data.DatabaseHelper(this);

        // Set up the toolbar
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        // Set up RecyclerView
        recyclerViewTags.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RfidTagAdapter(scannedTags);
        recyclerViewTags.setAdapter(adapter);

        // Handle scan button click (software button) - immediate response like hardware
        buttonScan.setOnClickListener(v -> {
            handleHardwareButtonPress(); // Use same immediate response as hardware button
        });
        
        // Handle set limit button click
        buttonSetLimit.setOnClickListener(v -> {
            showSetLimitDialog();
        });
        
        // Handle save button click
        buttonSave.setOnClickListener(v -> {
            if (isScanning) {
                stopScanning();
            }
            if (scannedTags.size() > tagLimit) {
                Toast.makeText(RFIDScanActivity.this, "Cannot save data. Data is greater than limit (" + tagLimit + ")", Toast.LENGTH_LONG).show();
                return;
            }
            // Show location and status dialog
            showLocationStatusDialog();
        });
        
        // Handle discard button click
        buttonDiscard.setOnClickListener(v -> {
            if (isScanning) {
                stopScanning();
            }
            // Clear all tags
            scannedTags.clear();
            tagInfoMap.clear();
            adapter.notifyDataSetChanged();
            // Reset counters
            tagCount = 0;
            textScannedTagsLabel.setText("SCANNED TAGS (0)");
            textScanStatus.setText("Data Discarded");
            textScanMessage.setText("All tags have been cleared");
            textScanStatus.setTextColor(getResources().getColor(R.color.danger_red, getTheme()));
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.rfid_scan), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private long startTime = 0;
    private boolean keyUpFalg = true;

    @Override
    public void onStart() {
        super.onStart();
        // Removed redundant receiver registration (moved to onResume for consistency)
    }

    @Override
    public void onStop() {
        super.onStop();
        if (isScanning) {
            stopScanning();
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        try {
            if (keyCode == KeyEvent.KEYCODE_F4) {

            } else if (keyCode == KeyEvent.KEYCODE_BACK) {
                startStop(false);
                if (activity != null) {
                    activity.getApplicationContext().unregisterReceiver(keyReceiver);
                }
                Intent myIntent = new Intent(RFIDScanActivity.this, DashboardActivity.class);
                startActivity(myIntent);
                finish();
                return true;
            }
        } catch (RuntimeException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return super.onKeyUp(keyCode, event);
    }

    public void startStop(boolean isStartInventory) {
        if (mUhfrManager == null) {
            Toast.makeText(this, getString(R.string.connection_failed), Toast.LENGTH_SHORT).show();
            return;
        }

        if (isStartInventory) {
            isStart = true;
            mUhfrManager.setGen2session(isMulti); 
            if (isMulti) {
                mUhfrManager.asyncStartReading();
            }
            handler.postDelayed(runnable_MainActivity, 0);
        } else {
            isStart = false;
            if (isMulti) {
                mUhfrManager.asyncStopReading();
            }
            handler.removeCallbacks(runnable_MainActivity);
        }
    }

    // continuous scan logic
    private Runnable runnable_MainActivity = new Runnable() {
        @Override
        public void run() {
            if (!isStart) return;
            
            try {
                if (mUhfrManager != null) {
                    List<Reader.TAGINFO> tagList = mUhfrManager.tagInventoryRealTime();
                    if (tagList != null && !tagList.isEmpty()) {
                        readTagAction(tagList);
                        handler.sendEmptyMessage(MSG_INVENROTY);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error in runnable_MainActivity: " + e.getMessage());
            }
            
            if (isStart) {
                handler.postDelayed(runnable_MainActivity, 40); // Standard PDA polling interval
            }
        }
    };

    private Handler handler1 = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    String epc = msg.getData().getString("data");
                    String rssi = msg.getData().getString("rssi");
                    break;
                case 1980:
                    break;
            }
        }
    };

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // Handle back button press
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        android.util.Log.d(TAG, "onResume called");
        
        IntentFilter keyFilter = new IntentFilter();
        keyFilter.addAction("android.rfid.FUN_KEY");
        keyFilter.addAction("com.rfid.SCAN_KEY");
        keyFilter.addAction("android.intent.action.FUN_KEY");
        keyFilter.addAction("com.android.server.scannerservice.broadcast");
        
        // PDA hardware broadcasts sometimes need receiver exported
        if (android.os.Build.VERSION.SDK_INT >= 34) {
             registerReceiver(keyReceiver, keyFilter, Context.RECEIVER_EXPORTED);
        } else {
             registerReceiver(keyReceiver, keyFilter);
        }
        android.util.Log.d(TAG, "Key receiver registered in onResume");
    }

    @Override
    protected void onPause() {
        super.onPause();
        android.util.Log.d(TAG, "onPause called");
        
        // Stop scanning to release resources and prevent conflicts
        if (isScanning) {
            stopScanning();
        }
        
        // Unregister key receiver
        try {
            unregisterReceiver(keyReceiver);
            android.util.Log.d(TAG, "Key receiver unregistered");
        } catch (Exception e) {
            // Ignore
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        android.util.Log.d(TAG, "onDestroy called");
        // Stop scanning if active
        if (isScanning) {
            stopScanning();
        }
        // Release wake lock
        releaseWakeLock();
        
        // Clean up direct API connection
        if (mUhfrManager != null) {
            mUhfrManager.close();
            mUhfrManager = null;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        android.util.Log.d(TAG, "Key down: " + keyCode);
        
        // Handle common RFID scan keys for handheld devices
        // 131-135 are common for F1-F5, 280-295 are often used for side/trigger buttons
        if ((keyCode >= 131 && keyCode <= 140) || 
            (keyCode >= 280 && keyCode <= 295) ||
            keyCode == KeyEvent.KEYCODE_F4 || 
            keyCode == KeyEvent.KEYCODE_BUTTON_R1 || 
            keyCode == KeyEvent.KEYCODE_BUTTON_L1 ||
            keyCode == 523 || keyCode == 524) {
            
            if (event.getRepeatCount() == 0) {
                handleHardwareButtonPress();
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    // Initialize power management for continuous scanning
    private void initPowerManagement() {
        try {
            PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (powerManager != null) {
                wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "RFIDScan::Scanner");
                android.util.Log.d(TAG, "Power management initialized");
            }
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error initializing power management: " + e.getMessage());
        }
    }
    
    // Acquire wake lock for continuous scanning
    private void acquireWakeLock() {
        if (wakeLock != null && !wakeLock.isHeld()) {
            wakeLock.acquire(60 * 60 * 1000L); // 1 hour timeout
            android.util.Log.d(TAG, "Wake lock acquired");
        }
    }
    
    // Release wake lock
    private void releaseWakeLock() {
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
            android.util.Log.d(TAG, "Wake lock released");
        }
    }
    
    private void initModule() {
        android.util.Log.d(TAG, "initModule called");
        mUhfrManager = UHFRManager.getInstance();// Init Uhf module
        if(mUhfrManager!=null){
            //5106和6106 /6107和6108 支持33db
            sharedUtil = new SharedUtil(this);
            Reader.READER_ERR err = mUhfrManager.setPower(sharedUtil.getPower(), sharedUtil.getPower());//set uhf module power

            if(err== Reader.READER_ERR.MT_OK_ERR){
                rfidIsActive = true;
                Reader.READER_ERR err1 = mUhfrManager.setRegion(Reader.Region_Conf.valueOf(sharedUtil.getWorkFreq()));

                setParam() ;
                Log.d(TAG, "RFID Module Initialized successfully");
            }else {
                //5101 30db
                Reader.READER_ERR err1 = mUhfrManager.setPower(30, 30);//set uhf module power
                if(err1== Reader.READER_ERR.MT_OK_ERR) {
                    rfidIsActive = true ;
                    mUhfrManager.setRegion(Reader.Region_Conf.valueOf(sharedUtil.getWorkFreq()));
                    setParam() ;
                    Log.d(TAG, "RFID Module Initialized with fallback power (30dBm)");
                }else {
                    rfidIsActive = false;
                    Toast.makeText(this, "RFID module init failed", Toast.LENGTH_SHORT).show();
                }
            }

        }else {
            rfidIsActive = false;
            Toast.makeText(this, "RFID module init failed", Toast.LENGTH_SHORT).show();
        }
    }


    private void setParam() {
        if (mUhfrManager != null && sharedUtil != null) {
            //session
            mUhfrManager.setGen2session(sharedUtil.getSession());
            //taget
            mUhfrManager.setTarget(sharedUtil.getTarget());
            //q value
            mUhfrManager.setQvaule(sharedUtil.getQvalue());
            Log.d(TAG, "RFID Parameters set: Session=" + sharedUtil.getSession());
        }
    }





    public String secToTime(long time) {
        formatter.setTimeZone(TimeZone.getTimeZone("GMT+00:00"));
        time = time * 1000;
        String hms = formatter.format(time);
        return hms;
    }







    
    // Handle hardware button press (immediate response for both hardware and software buttons)
    private void handleHardwareButtonPress() {
        android.util.Log.d(TAG, "Button pressed - immediate toggle (hardware/software)");
        toggleScanning();
    }
    
    // Toggle scanning on/off
    private void toggleScanning() {
        if(mUhfrManager == null){
            Toast.makeText(activity, "Timeout", Toast.LENGTH_SHORT).show();
//            showToast(R.string.communication_timeout);
            return ;
        }
        if (isScanning) {
            stopScanning();
        } else {
            startScanning();
        }
    }
    
    // Start continuous scanning
    private void startScanning() {
        if (!rfidIsActive || mUhfrManager == null) {
            Toast.makeText(this, "RFID reader not ready", Toast.LENGTH_SHORT).show();
            return;
        }

        isScanning = true;
        buttonScan.setText("Stop Scanning");
        textScanStatus.setText("Scanning Active");
        textScanMessage.setText("Ready to detect RFID tags.");
        textScanStatus.setTextColor(getResources().getColor(R.color.success_green, getTheme()));
            
        acquireWakeLock();
            
        // Start reader
        isMulti = true;
        startStop(true);
            
        if (timer == null) {
            timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    handler.sendEmptyMessage(MSG_INVENROTY_TIME);
                }
            }, 1000, 1000);
        }

        Toast.makeText(this, "Scanning started", Toast.LENGTH_SHORT).show();
    }
        
    // Stop continuous scanning
    private void stopScanning() {
        isScanning = false;
        buttonScan.setText("Start Scanning");
        textScanStatus.setText("Scanning Stopped");
        textScanMessage.setText("Scanning paused. Click to resume or use hardware button.");
        textScanStatus.setTextColor(getResources().getColor(R.color.medium_grey, getTheme()));
            
        releaseWakeLock();
            
        if (timer != null) {
            timer.cancel();
            timer = null;
        }

        startStop(false);
        Toast.makeText(this, "Scanning stopped", Toast.LENGTH_SHORT).show();
    }
    
    // Process tag list and update UI
    private void readTagAction(List<com.uhf.api.cls.Reader.TAGINFO> tagList) {
        if (tagList != null && !tagList.isEmpty()) {
            for (com.uhf.api.cls.Reader.TAGINFO tagInfo : tagList) {
                pooled6cData(tagInfo);
            }
        }
    }
    
    private boolean pooled6cData(Reader.TAGINFO info) {
        String epc = extractEpcFromTagInfo(info);
        if (epc == null) return false;
        
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        String timestamp = sdf.format(new Date());

        // Play beep sound
        UtilSound.play(1, 0);

        if (tagInfoMap.containsKey(epc)) {
            RfidTag tag = tagInfoMap.get(epc);
            tag.setCount(tag.getCount() + 1);
            tag.setTimestamp(timestamp);
            return true;
        } else {
            // Check if tag exists in database
            boolean isFound = databaseHelper.isTagIdInProducts(epc);
            String status = isFound ? "Found" : "Not Found";
            
            RfidTag tag = new RfidTag(epc, timestamp, status);
            tagInfoMap.put(epc, tag);
            return true;
        }
    }
    
    // Extract EPC from tag info object
    private String extractEpcFromTagInfo(com.uhf.api.cls.Reader.TAGINFO tagInfo) {
        try {
            if (tagInfo != null && tagInfo.EpcId != null) {
                return Tools.Bytes2HexString(tagInfo.EpcId, tagInfo.EpcId.length);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error extracting EPC: " + e.getMessage());
        }
        return null;
    }

    // Check if RFID reader is connected
    private boolean isConnected() {
        if (mUhfrManager != null) {
             return true; // If we have an instance, we treat it as connected (initialization happened)
        }
        return false;
    }

    public void onStopReader() {
        Log.e(TAG, "[onStopReader] start");
        try {
            if (mUhfrManager != null) {
                mUhfrManager.close();
                mUhfrManager = null;
            }
            // Power off module if possible
            com.gg.reader.api.utils.XhPower.setPower(false);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.e(TAG, "[onStopReader] end");
    }

//    =============== Setting Limit Dialog Box =========
    private void showSetLimitDialog() {
        Dialog dialog = new Dialog(this, R.style.Theme_RFID_Demo_Dialog);
        dialog.setContentView(R.layout.dialog_set_limit);
        
        TextInputEditText editTextLimit = dialog.findViewById(R.id.editTextLimit);
        Button buttonCancel = dialog.findViewById(R.id.buttonCancel);
        Button buttonSave = dialog.findViewById(R.id.buttonSave);
        
        editTextLimit.setText(String.valueOf(tagLimit));
        
        buttonCancel.setOnClickListener(v -> dialog.dismiss());
        buttonSave.setOnClickListener(v -> {
            String limitStr = editTextLimit.getText().toString().trim();
            if (limitStr.isEmpty()) {
                editTextLimit.setError("Limit is required");
                return;
            }
            try {
                int newLimit = Integer.parseInt(limitStr);
                if (newLimit <= 0) {
                    editTextLimit.setError("Limit must be greater than 0");
                    return;
                }
                tagLimit = newLimit;
                dialog.dismiss();
            } catch (NumberFormatException e) {
                editTextLimit.setError("Invalid limit value");
            }
        });
        dialog.show();
    }

//    ========== Location status Dialog box ==========
    private void showLocationStatusDialog() {
        Dialog dialog = new Dialog(this, R.style.Theme_RFID_Demo_Dialog);
        dialog.setContentView(R.layout.dialog_location_status);
        
        TextInputEditText editTextLocation = dialog.findViewById(R.id.editTextLocation);
        AutoCompleteTextView autoCompleteStatus = dialog.findViewById(R.id.autoCompleteStatus);
        Button buttonCancel = dialog.findViewById(R.id.buttonCancel);
        Button buttonSave = dialog.findViewById(R.id.buttonSave);
        
        String[] statuses = {"Active", "Disable"};
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, statuses);
        autoCompleteStatus.setAdapter(statusAdapter);
        
        buttonCancel.setOnClickListener(v -> dialog.dismiss());
        buttonSave.setOnClickListener(v -> {
            String location = editTextLocation.getText().toString().trim();
            String status = autoCompleteStatus.getText().toString().trim();
            
            if (location.isEmpty()) {
                editTextLocation.setError("Location is required");
                return;
            }
            
            if (status.isEmpty() || !(status.equals("Active") || status.equals("Disable"))) {
                autoCompleteStatus.setError("Please select a valid status");
                return;
            }
            
            // Save only "Found" tags to database
            // Use ISO 8601 format for server compatibility
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            String scanTime = sdf.format(new Date());
            
            int savedCount = 0;

            for (RfidTag tag : scannedTags) {
                // Use the status from the tag itself (Found/Not Found)
                String currentStatus = tag.getStatus();
                if (currentStatus == null || currentStatus.isEmpty()) {
                    currentStatus = "Not Found";
                }
                
                // Skip if already saved
                if (currentStatus.startsWith("Saved")) {
                    continue;
                }
                
                // Skip if not "Found"
                if (!"Found".equals(currentStatus)) {
                    continue;
                }
                
                ScanLog scanLog = new ScanLog(0, tag.getTagId(), scanTime, location, status);
                long result = databaseHelper.insertScanLog(scanLog);
                if (result > 0) {
                    savedCount++;
                    tag.setStatus("Saved - " + status + " (" + location + ")");
                }
            }
            
            // Update adapter to reflect saved status
            adapter.notifyDataSetChanged();
            
            // Calculate total saved tags (including previously saved ones)
            int totalSaved = 0;
            for (RfidTag tag : scannedTags) {
                if (tag.getStatus() != null && tag.getStatus().startsWith("Saved")) {
                    totalSaved++;
                }
            }
            
            int newlySaved = savedCount;
            // Calculate skipped "Not Found" tags
            int skippedCount = 0;
             for (RfidTag tag : scannedTags) {
                String s = tag.getStatus();
                if (s == null || (!s.startsWith("Saved") && !"Found".equals(s))) {
                    skippedCount++;
                }
            }
            
            String message = newlySaved + " new tag(s) saved";
            if (newlySaved == 0) {
                if (totalSaved > 0 && skippedCount == 0) {
                     message = "All found tags already saved";
                } else if (skippedCount > 0) {
                    message = "0 saved (" + skippedCount + " skipped)";
                } else {
                     message = "No found tags to save";
                }
            } else if (skippedCount > 0) {
                 message += " (" + skippedCount + " skipped)";
            }
            
            textScanStatus.setText("Data Saved");
            textScanMessage.setText(message + ". Loc: " + location + ", Sts: " + status);
            textScanStatus.setTextColor(getResources().getColor(R.color.success_green, getTheme()));
            
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            
            // Check if upload mode is enabled and trigger upload
            if (isUploadModeEnabled()) {
                triggerDataUpload();
            }
            
            dialog.dismiss();
        });
        dialog.show();
    }
    
    // Check if upload mode is enabled from SharedPreferences
    private boolean isUploadModeEnabled() {
        android.content.SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        return prefs.getBoolean("upload_mode_enabled", false);
    }
    
    // Trigger data upload service
    private void triggerDataUpload() {
        android.content.SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String jwtToken = prefs.getString("jwt_token", null);
        
        if (jwtToken != null && !jwtToken.isEmpty()) {
            Intent uploadIntent = new Intent(this, com.ecartes.rfid_demo.service.DataUploadService.class);
            uploadIntent.putExtra("jwt_token", jwtToken);
            startService(uploadIntent);
            Toast.makeText(this, "Uploading data to server...", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Upload failed: No authentication token", Toast.LENGTH_SHORT).show();
        }
    }
}
