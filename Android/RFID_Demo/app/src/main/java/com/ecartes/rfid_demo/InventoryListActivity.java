package com.ecartes.rfid_demo;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import java.util.ArrayList;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ecartes.rfid_demo.data.DatabaseHelper;
import com.ecartes.rfid_demo.adapter.ScanLogAdapter;
import com.ecartes.rfid_demo.model.ScanLog;
import com.google.android.material.appbar.MaterialToolbar;
import java.util.List;

public class InventoryListActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private Spinner spinnerDateRange;
    private Spinner spinnerCategory;
    private RecyclerView recyclerViewInventory;
    private TextView textRecordsCount;
    private ScanLogAdapter scanLogAdapter;
    private boolean isInitialSetup = true;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_inventory_list);

        // Initialize database helper
        dbHelper = new DatabaseHelper(this);
        
        // Find views
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        recyclerViewInventory = findViewById(R.id.recyclerViewInventory);
        spinnerDateRange = findViewById(R.id.spinnerDateRange);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        textRecordsCount = findViewById(R.id.textRecordsCount);

        // Set up the toolbar
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        
        // Set up RecyclerView with adapter
        recyclerViewInventory.setLayoutManager(new LinearLayoutManager(this));
        scanLogAdapter = new ScanLogAdapter(new ArrayList<>());
        recyclerViewInventory.setAdapter(scanLogAdapter);
        
        // Populate spinners
        populateDateSpinner();
        populateStatusSpinner();
        
        // Initially set no selection
        spinnerDateRange.setSelection(0);
        spinnerCategory.setSelection(0);
        
        // Initially load all data
        loadFilteredData();
        
        // Set up Load button
        Button buttonLoad = findViewById(R.id.buttonLoad);
        buttonLoad.setOnClickListener(v -> loadFilteredData());
        
        // Now that initial setup is complete, allow user selections to trigger updates
        isInitialSetup = false;

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.inventory_list), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
    
    private void populateDateSpinner() {
        List<String> dates = dbHelper.getDistinctScanDates();
        
        // Create list with "All" option as the first item
        List<String> dateList = new ArrayList<>();
        dateList.add("All"); // Default option
        dateList.addAll(dates);
        
        // Create adapter with dates
        ArrayAdapter<String> dateAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, dateList);
        dateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        
        // If no dates are available, show "All" and "No data available"
        if (dates.isEmpty()) {
            dateList.clear();
            dateList.add("All");
            dateList.add("No data available");
            dateAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, dateList);
            dateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        }
        
        spinnerDateRange.setAdapter(dateAdapter);
        
        // Set listener to handle date selection
        spinnerDateRange.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!isInitialSetup) {
                    loadFilteredData();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
    }
    
    private void populateStatusSpinner() {
        // Create adapter with "All" option and status options
        List<String> statusList = new ArrayList<>();
        statusList.add("All"); // Default option
        statusList.add("Active");
        statusList.add("Disable");
        
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, statusList);
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        
        spinnerCategory.setAdapter(statusAdapter);
        
        // Enable the spinner so user can select from options
        spinnerCategory.setEnabled(true);
        
        // Set listener to handle status selection
        spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!isInitialSetup) {
                    loadFilteredData();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
    }
    
    private void loadFilteredData() {
        String selectedDate = spinnerDateRange.getSelectedItem() != null ? 
            spinnerDateRange.getSelectedItem().toString() : null;
        String selectedStatus = spinnerCategory.getSelectedItem() != null ? 
            spinnerCategory.getSelectedItem().toString() : null;
        
        // Handle "All" and "No data available" options
        if (selectedDate != null && (selectedDate.equals("All") || selectedDate.equals("No data available"))) {
            selectedDate = null;
        }
        
        if (selectedStatus != null && selectedStatus.equals("All")) {
            selectedStatus = null;
        }
        
        // Fetch filtered data from database
        List<ScanLog> scanLogs = dbHelper.getFilteredScanLogs(selectedDate, selectedStatus);
        
        // Update adapter with filtered data
        scanLogAdapter.updateScanLogs(scanLogs);
        
        // Update records count
        textRecordsCount.setText(String.valueOf(scanLogs.size()));
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