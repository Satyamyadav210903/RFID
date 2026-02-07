package com.ecartes.rfid_demo.api;

import com.ecartes.rfid_demo.model.ApiResponse;
import com.ecartes.rfid_demo.model.LoginRequest;
import com.ecartes.rfid_demo.model.LoginResponse;
import com.ecartes.rfid_demo.model.Product;
import com.ecartes.rfid_demo.model.ScanLog;
import com.ecartes.rfid_demo.model.ScanRequest;
import com.ecartes.rfid_demo.model.User;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface ApiService {
    
    // Authentication endpoints
    @POST("api/auth/login")
    Call<LoginResponse> login(@Body LoginRequest loginRequest);

    // Product endpoints
    @GET("api/rfid/products")
    Call<List<Product>> getAllProducts(@Header("Authorization") String token);


    // Scan log endpoints
    @GET("api/rfid/scanlogs")
    Call<List<ScanLog>> getAllScanLogs(@Header("Authorization") String token);

//    @POST("api/rfid/scanlogs")
//    Call<ApiResponse<ScanLog>> createScanLog(@Header("Authorization") String token, @Body ScanLog scanLog);

    @POST("api/rfid/scanlogs")
    Call<ScanLog> createScanLog(
            @Header("Authorization") String token,
            @Body ScanLog scanLog
    );
}