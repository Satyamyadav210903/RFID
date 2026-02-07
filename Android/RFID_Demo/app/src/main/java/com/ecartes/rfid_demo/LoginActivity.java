package com.ecartes.rfid_demo;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.ecartes.rfid_demo.api.ApiClient;
import com.ecartes.rfid_demo.api.ApiService;
import com.ecartes.rfid_demo.data.DatabaseHelper;
import com.ecartes.rfid_demo.model.ApiResponse;
import com.ecartes.rfid_demo.model.LoginRequest;
import com.ecartes.rfid_demo.model.LoginResponse;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "UserPrefs";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_JWT_TOKEN = "jwt_token";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_REMEMBER_ME = "remember_me";

    private DatabaseHelper dbHelper;
    private ApiService apiService;
    private ProgressBar progressBar;
    private Button buttonLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        // ✅ FIX: Correct layout for LoginActivity
        setContentView(R.layout.activity_main);

        // Initialize ApiClient with context to load saved server URL
        ApiClient.initialize(this);
        
        dbHelper = new DatabaseHelper(this);
        apiService = ApiClient.getApiService();

        progressBar = findViewById(R.id.progressBar);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        TextInputEditText editTextUsername = findViewById(R.id.editTextUsername);
        TextInputEditText editTextPassword = findViewById(R.id.editTextPassword);
        buttonLogin = findViewById(R.id.buttonLogin);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("RFID Scanner");
        }

        // Auto-login check
        checkAutoLogin();

        buttonLogin.setOnClickListener(v -> {
            String username = editTextUsername.getText().toString().trim();
            String password = editTextPassword.getText().toString().trim();

            if (username.isEmpty()) {
                editTextUsername.setError("Username is required");
                return;
            }

            if (password.isEmpty()) {
                editTextPassword.setError("Password is required");
                return;
            }

            performApiLogin(username, password);
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.login), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    // --------------------------------------------------
    // AUTO LOGIN
    // --------------------------------------------------
    private void checkAutoLogin() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String jwtToken = prefs.getString(KEY_JWT_TOKEN, null);
        String username = prefs.getString(KEY_USERNAME, null);
        String password = prefs.getString(KEY_PASSWORD, null);
        boolean rememberMe = prefs.getBoolean(KEY_REMEMBER_ME, false);

        // If we have valid credentials and remember me is enabled, try auto-login
        if (rememberMe && username != null && !username.isEmpty() && password != null && !password.isEmpty()) {
            // Auto-login with stored credentials
            performApiLogin(username, password);
        } else if (jwtToken != null && !jwtToken.isEmpty()) {
            // If we have a valid JWT token, go to dashboard
            startActivity(new Intent(this, DashboardActivity.class));
            finish();
        }
    }

    private void saveLoggedInUser(int userId, String jwtToken, String username, String password, boolean rememberMe) {
        SharedPreferences.Editor editor =
                getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
        editor.putInt(KEY_USER_ID, userId);
        editor.putString(KEY_JWT_TOKEN, jwtToken); // ✅ RAW token only
        
        if (rememberMe) {
            editor.putString(KEY_USERNAME, username);
            editor.putString(KEY_PASSWORD, password);
            editor.putBoolean(KEY_REMEMBER_ME, true);
        } else {
            // Clear stored credentials if remember me is disabled
            editor.remove(KEY_USERNAME);
            editor.remove(KEY_PASSWORD);
            editor.remove(KEY_REMEMBER_ME);
        }
        
        editor.apply();
    }

    public void clearLoggedInUser() {
        SharedPreferences.Editor editor =
                getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
        editor.clear();
        editor.apply();
    }

    // --------------------------------------------------
    // LOGIN API
    // --------------------------------------------------
    private void performApiLogin(String username, String password) {
        progressBar.setVisibility(android.view.View.VISIBLE);
        buttonLogin.setEnabled(false);

        LoginRequest request = new LoginRequest(username, password);
        apiService.login(request).enqueue(new Callback<LoginResponse>() {

            @Override
            public void onResponse(Call<LoginResponse> call,
                                   Response<LoginResponse> response) {

                progressBar.setVisibility(android.view.View.GONE);
                buttonLogin.setEnabled(true);

                if (response.isSuccessful() && response.body() != null) {
                    String jwtToken = response.body().getToken();
                    
                    if (jwtToken != null && !jwtToken.isEmpty()) {
                        // ✅ FIX: store RAW token and credentials
                        saveLoggedInUser(0, jwtToken, username, password, true); // Always remember for auto-login

                        // Sync data after login
                        fetchAndStoreData("Bearer " + jwtToken);

                        Toast.makeText(LoginActivity.this,
                                "Login Successful", Toast.LENGTH_SHORT).show();

                        startActivity(new Intent(LoginActivity.this,
                                DashboardActivity.class));
                        finish();
                        return;
                    }
                }
                
                // Handle login failure
                if (response.code() == 401) {
                    Toast.makeText(LoginActivity.this,
                            "Invalid username or password",
                            Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(LoginActivity.this,
                            "Login failed: " + response.code() + " - " + (response.message() != null ? response.message() : "Unknown error"),
                            Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                progressBar.setVisibility(android.view.View.GONE);
                buttonLogin.setEnabled(true);

                Toast.makeText(LoginActivity.this,
                        "Network error: " + t.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    // --------------------------------------------------
    // DATA SYNC
    // --------------------------------------------------
    private void fetchAndStoreData(String token) {
        fetchAndStoreProducts(token);
        fetchAndStoreScanLogs(token);
    }

    private void fetchAndStoreProducts(String token) {
        // Fetch products directly as array
        apiService.getAllProducts(token).enqueue(new Callback<java.util.List<com.ecartes.rfid_demo.model.Product>>() {
            @Override
            public void onResponse(Call<java.util.List<com.ecartes.rfid_demo.model.Product>> call,
                                   Response<java.util.List<com.ecartes.rfid_demo.model.Product>> response) {

                if (response.isSuccessful() && response.body() != null) {
                    dbHelper.insertProductsFromApi(response.body());
                }
            }

            @Override
            public void onFailure(Call<java.util.List<com.ecartes.rfid_demo.model.Product>> call,
                                  Throwable t) {
                // Silent failure for background sync
            }
        });
    }

    private void fetchAndStoreScanLogs(String token) {
        // Fetch scan logs directly as array
        apiService.getAllScanLogs(token).enqueue(new Callback<java.util.List<com.ecartes.rfid_demo.model.ScanLog>>() {
            @Override
            public void onResponse(Call<java.util.List<com.ecartes.rfid_demo.model.ScanLog>> call,
                                   Response<java.util.List<com.ecartes.rfid_demo.model.ScanLog>> response) {

                if (response.isSuccessful() && response.body() != null) {
                    dbHelper.insertScanLogsFromApi(response.body());
                }
            }

            @Override
            public void onFailure(Call<java.util.List<com.ecartes.rfid_demo.model.ScanLog>> call,
                                  Throwable t) {
                // Silent failure for background sync
            }
        });
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.options_menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_server_config) {
            showServerConfigDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    private void showServerConfigDialog() {
        Dialog dialog = new Dialog(this, R.style.Theme_RFID_Demo_Dialog);
        dialog.setContentView(R.layout.dialog_server_config);
        
        // Find dialog views
        com.google.android.material.textfield.TextInputEditText editTextServerUrl = 
            dialog.findViewById(R.id.editTextServerUrl);
        TextView textCurrentUrl = dialog.findViewById(R.id.textCurrentUrl);
        android.widget.Button buttonCancel = dialog.findViewById(R.id.buttonCancel);
        android.widget.Button buttonSave = dialog.findViewById(R.id.buttonSave);
        
        // Set current URL
        String currentUrl = com.ecartes.rfid_demo.api.ApiClient.getBaseUrl();
        textCurrentUrl.setText("Current URL: " + currentUrl);
        editTextServerUrl.setText(currentUrl);
        
        // Handle cancel button
        buttonCancel.setOnClickListener(v -> dialog.dismiss());
        
        // Handle save button
        buttonSave.setOnClickListener(v -> {
            String serverUrl = editTextServerUrl.getText().toString().trim();
            
            if (serverUrl.isEmpty()) {
                editTextServerUrl.setError("Server URL is required");
                return;
            }
            
            // Validate URL format
            if (!isValidUrl(serverUrl)) {
                editTextServerUrl.setError("Please enter a valid URL");
                return;
            }
            
            // Ensure URL ends with /
            if (!serverUrl.endsWith("/")) {
                serverUrl = serverUrl + "/";
            }
            
            // Save the new URL
            com.ecartes.rfid_demo.api.ApiClient.setBaseUrl(serverUrl);
            
            // Save to shared preferences for persistence
            SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
            editor.putString("server_url", serverUrl);
            editor.apply();
            
            Toast.makeText(LoginActivity.this, "Server URL updated successfully", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
        
        dialog.show();
    }
    
    private boolean isValidUrl(String url) {
        try {
            java.net.URL urlObj = new java.net.URL(url);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
