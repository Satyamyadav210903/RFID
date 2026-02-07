package com.ecartes.rfid_demo;

import com.gg.reader.api.utils.XhPower;

import android.app.Dialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.KeyEvent;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ecartes.rfid_demo.model.RfidTag;
import com.ecartes.rfid_demo.utils.SharedUtil;
import com.ecartes.rfid_demo.utils.Tools;
import com.ecartes.rfid_demo.utils.UtilSound;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.handheld.uhfr.UHFRManager;
import com.uhf.api.cls.Reader;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Pattern;

public class RFIDWriteActivity extends AppCompatActivity {

    private final String TAG = "RFIDWriteActivity";

    // UI Components
    private Button buttonScan;
    private Button buttonWrite;
    private TextView textScanStatus;
    private TextView textTargetTag;
    private TextInputEditText editTextWriteData;
    private CheckBox checkBoxFilter;
    private RecyclerView recyclerViewTags;
    private RfidTagAdapter adapter;
    private ImageButton buttonCopy;
    private ImageButton buttonPaste;
    private TextView textScannedTagsLabel;
    private TextView textScanMessage;

    // Data
    private List<RfidTag> scannedTags;
    private Map<String, RfidTag> tagInfoMap = new java.util.LinkedHashMap<>();
    private String selectedEpc = null;
    private int tagCount = 0;
    
    // RFID Manager
    private UHFRManager mUhfrManager;
    private SharedUtil sharedUtil;
    private boolean isScanning = false;
    private boolean isMulti = false; // continuous scan flag
    private Timer timer;
    private int time = 0;
    
    // Write parameters
    private int membank = 1; // EPC bank
    private int startAddr = 2; // Start address for EPC
    private byte[] accessPassword = new byte[4]; // Default 00000000

    private final int MSG_INVENROTY = 1 ;

    private final int MSG_INVENROTY_TIME = 1001 ;
    private Handler handler = new Handler(Looper.getMainLooper()) {
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
                    if(textScannedTagsLabel != null)
                        textScannedTagsLabel.setText("SCANNED TAGS (" + tagCount + ")");
                    if (!scannedTags.isEmpty() && textScanMessage != null) {
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

    // Aggressive Key Handling
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
           Log.d(TAG, "dispatchKeyEvent DOWN: " + keyCode);
           if (isScanKey(keyCode) && event.getRepeatCount() == 0) {
               handleHardwareButtonPress();
               return true; // Consume
           }
        } else if (event.getAction() == KeyEvent.ACTION_UP) {
            Log.d(TAG, "dispatchKeyEvent UP: " + keyCode);
            if (isScanKey(keyCode)) {
                return true; // Consume
            }
        }
        return super.dispatchKeyEvent(event);
    }
    
    private boolean isScanKey(int keyCode) {
        return (keyCode >= 131 && keyCode <= 140) || 
               (keyCode >= 280 && keyCode <= 295) ||
               keyCode == KeyEvent.KEYCODE_F4 || 
               keyCode == KeyEvent.KEYCODE_BUTTON_R1 || 
               keyCode == KeyEvent.KEYCODE_BUTTON_L1 ||
               keyCode == 523 || keyCode == 524;
    }

    private void handleHardwareButtonPress() {
        Log.d(TAG, "Handle Hardware Button Press (Triggered)");
        toggleScanning();
    }

    // Key receiver for hardware button (Broadcast Backup)
    private BroadcastReceiver keyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int keyCode = intent.getIntExtra("keyCode", 0);
            if (keyCode == 0) {
                keyCode = intent.getIntExtra("keycode", 0);
            }
            boolean keyDown = intent.getBooleanExtra("keydown", false);

            if (!keyDown) {
                android.util.Log.d(TAG, "Hardware Key Up (Receiver): " + keyCode);
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
    
    // onKeyDown/Up delegated to dispatchKeyEvent logic or super
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Log.d(TAG, "onKeyDown: " + keyCode);
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        // Log.d(TAG, "onKeyUp: " + keyCode);
        return super.onKeyUp(keyCode, event);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rfid_write);
        
        sharedUtil = new SharedUtil(this);
        scannedTags = new ArrayList<>();
        
        // Init UI
        initView();
        
        // Init Sound
        UtilSound.initSoundPool(this);
        
        // Init RFID
        initModule();
    }
    
    private void initView() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        buttonScan = findViewById(R.id.buttonScan);
        buttonWrite = findViewById(R.id.buttonWrite);
        textScanStatus = findViewById(R.id.textScanStatus);
        textTargetTag = findViewById(R.id.textTargetTag);
        editTextWriteData = findViewById(R.id.editTextWriteData);
        checkBoxFilter = findViewById(R.id.checkBoxFilter);
        recyclerViewTags = findViewById(R.id.recyclerViewTags);
        buttonCopy = findViewById(R.id.buttonCopy);
        buttonPaste = findViewById(R.id.buttonPaste);
        Button buttonClear = findViewById(R.id.buttonClear);

        // Setup RecyclerView
        recyclerViewTags.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RfidTagAdapter(scannedTags);
        recyclerViewTags.setAdapter(adapter);
        
        // Handle Item Click (Select Tag)
        adapter.setOnItemClickListener(tag -> {
            selectedEpc = tag.getTagId();
            textTargetTag.setText(selectedEpc);
            checkBoxFilter.setEnabled(true);
            checkBoxFilter.setChecked(true); // Auto-enable filter when tag selected
            Toast.makeText(this, "Selected: " + selectedEpc, Toast.LENGTH_SHORT).show();
            
            // Stop scanning if selecting
            if (isScanning) {
                stopScanning();
            }
        });

        buttonScan.setOnClickListener(v -> toggleScanning());
        
        // Clear List Logic
        buttonClear.setOnClickListener(v -> {
            scannedTags.clear();
            tagInfoMap.clear();
            adapter.notifyDataSetChanged();
            tagCount = 0;
            if(textScannedTagsLabel != null)
                textScannedTagsLabel.setText("SCANNED TAGS (0)");
            
            selectedEpc = null;
            textTargetTag.setText("None Selected");
            checkBoxFilter.setChecked(false);
            checkBoxFilter.setEnabled(false);
            
            Toast.makeText(this, "List Cleared", Toast.LENGTH_SHORT).show();
        });
        
        buttonWrite.setOnClickListener(v -> write());
        
        // Copy listener
        buttonCopy.setOnClickListener(v -> {
            if (selectedEpc != null && !selectedEpc.isEmpty()) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("EPC", selectedEpc);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(this, "EPC copied to clipboard", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "No tag selected", Toast.LENGTH_SHORT).show();
            }
        });
        
        // Paste listener
        buttonPaste.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard.hasPrimaryClip() && clipboard.getPrimaryClipDescription().hasMimeType(android.content.ClipDescription.MIMETYPE_TEXT_PLAIN)) {
                ClipData.Item item = clipboard.getPrimaryClip().getItemAt(0);
                CharSequence text = item.getText();
                if (text != null) {
                    editTextWriteData.setText(text);
                }
            } else {
                Toast.makeText(this, "Clipboard is empty or not text", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    // Toggle scanning
    private void toggleScanning() {
        if (isScanning) {
            stopScanning();
        } else {
            startScanning();
        }
    }
    
    private void startScanning() {
        if (mUhfrManager == null) {
            Toast.makeText(this, "RFID Reader not initialized", Toast.LENGTH_SHORT).show();
            return;
        }
        
        isScanning = true;
        buttonScan.setText("Stop Scanning");
        textScanStatus.setText("Scanning...");
        
        // Clear previous list if needed? Maybe keep them.
        
        mUhfrManager.setGen2session(1); // Set session
        mUhfrManager.asyncStartReading();
        
        handler.postDelayed(runnable_MainActivity, 0);
        
        if (timer == null) {
            timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    handler.sendEmptyMessage(MSG_INVENROTY_TIME);
                }
            }, 1000, 1000);
        }
    }
    
    private void stopScanning() {
        isScanning = false;
        buttonScan.setText("Start Scanning");
        textScanStatus.setText("Ready to scan");
        
        if (mUhfrManager != null) {
            mUhfrManager.asyncStopReading();
        }
        handler.removeCallbacks(runnable_MainActivity);
        
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }
    
    private Runnable runnable_MainActivity = new Runnable() {
        @Override
        public void run() {
            if (!isScanning) return;
            try {
                if (mUhfrManager != null) {
                    List<Reader.TAGINFO> tagList = mUhfrManager.tagInventoryRealTime();
                    if (tagList != null && !tagList.isEmpty()) {
                        for (Reader.TAGINFO tagInfo : tagList) {
                            String epc = Tools.Bytes2HexString(tagInfo.EpcId, tagInfo.EpcId.length);
                            if (epc != null && !tagInfoMap.containsKey(epc)) {
                                RfidTag tag = new RfidTag(epc, "", "Scanned");
                                tagInfoMap.put(epc, tag);
                                UtilSound.play(1, 0);
                            }
                        }
                        handler.sendEmptyMessage(MSG_INVENROTY);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (isScanning) {
                handler.postDelayed(this, 50);
            }
        }
    };
    
    private void initModule() {
        try {
            mUhfrManager = UHFRManager.getInstance();
            if (mUhfrManager != null) {
                mUhfrManager.setPower(sharedUtil.getPower(), sharedUtil.getPower());
                mUhfrManager.setRegion(Reader.Region_Conf.valueOf(sharedUtil.getWorkFreq()));
            } else {
                Toast.makeText(this, "Failed to init RFID module", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    // Write Logic
    private void write() {
        if (!checkParam()) {
            return;
        }
        
        String writeDataStr = editTextWriteData.getText().toString().trim();
        if(!matchHex(writeDataStr) || writeDataStr.length() % 4 != 0) {
            Toast.makeText(this, "Please input valid Hex data (length % 4 == 0)", Toast.LENGTH_SHORT).show();
            return;
        }
        
        byte[] writeDataBytes = Tools.HexString2Bytes(writeDataStr);
        byte[] epcBytes = null;
        
        if (checkBoxFilter.isChecked()) {
             if (selectedEpc == null) {
                 Toast.makeText(this, "Please select a tag first", Toast.LENGTH_SHORT).show();
                 return;
             }
             epcBytes = Tools.HexString2Bytes(selectedEpc);
        }
        
        Reader.READER_ERR er;
        if (mUhfrManager == null) {
             Toast.makeText(this, "Reader not connected", Toast.LENGTH_SHORT).show();
             return;
        }

        try {
            if (checkBoxFilter.isChecked()) {
                Log.d(TAG, "Writing with filter: bank=" + membank + ", start=" + startAddr + ", ptr=1, len=2");
                er = mUhfrManager.writeTagDataByFilter((char)membank, startAddr, writeDataBytes, writeDataBytes.length, accessPassword, (short)1000, epcBytes, 1, 2, true);

            } else {
                er = mUhfrManager.writeTagData((char)membank, startAddr, writeDataBytes, writeDataBytes.length, accessPassword, (short)1000);
            }

            if (er == Reader.READER_ERR.MT_OK_ERR) {
                Toast.makeText(this, "Write Success!", Toast.LENGTH_SHORT).show();
                UtilSound.play(1, 0); // Success beep
                
                // Refresh list or update the item
                if (selectedEpc != null) {
                    scannedTags.clear();
                    tagInfoMap.clear();
                    adapter.notifyDataSetChanged();
                    textTargetTag.setText("None Selected (List Cleared)");
                    selectedEpc = null;
                }
            } else {
                Toast.makeText(this, "Write Failed", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Exception: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    // Check Params
    private boolean checkParam() {
        if (mUhfrManager == null) {
            return false;
        }
        if (editTextWriteData.getText().toString().trim().isEmpty()) {
            editTextWriteData.setError("Data required");
            return false;
        }
        return true;
    }
    
    // Regex for Hex
    private boolean matchHex(String str) {
        return Pattern.compile("^[0-9A-Fa-f]+$").matcher(str).matches();
    }
    
    @Override 
    protected void onResume() {
        super.onResume();
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
    }

    @Override 
    protected void onPause() {
        super.onPause();
        if (isScanning) stopScanning();
        
        try {
            unregisterReceiver(keyReceiver);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mUhfrManager != null) {
            mUhfrManager.close();
        }
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
