package com.ecartes.rfid_demo.api;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.concurrent.TimeUnit;

public class ApiClient {

    private static String BASE_URL = "http://192.168.1.8:5004/";
    private static Retrofit retrofit;
    private static android.content.Context appContext;

    private static OkHttpClient getOkHttpClient() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        return new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(logging)
                .build();
    }

    private static Retrofit buildRetrofit() {
        return new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(getOkHttpClient())
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    public static Retrofit getClient() {
        if (retrofit == null) {
            retrofit = buildRetrofit();
        }
        return retrofit;
    }

    public static ApiService getApiService() {
        return getClient().create(ApiService.class);
    }

    public static void setBaseUrl(String baseUrl) {
        BASE_URL = baseUrl;
        retrofit = buildRetrofit(); // rebuild properly
    }

    public static String getBaseUrl() {
        return BASE_URL;
    }
    
    public static void initialize(android.content.Context context) {
        appContext = context.getApplicationContext();
        // Load saved server URL if exists
        android.content.SharedPreferences prefs = appContext.getSharedPreferences("UserPrefs", android.content.Context.MODE_PRIVATE);
        String savedUrl = prefs.getString("server_url", null);
        if (savedUrl != null && !savedUrl.isEmpty()) {
            BASE_URL = savedUrl;
            retrofit = buildRetrofit(); // rebuild with saved URL
        }
    }
}
