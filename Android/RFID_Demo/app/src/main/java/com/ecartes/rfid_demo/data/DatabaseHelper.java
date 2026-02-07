package com.ecartes.rfid_demo.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.ecartes.rfid_demo.model.User;
import com.ecartes.rfid_demo.model.Product;
import com.ecartes.rfid_demo.model.ScanLog;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    
    private static final String TAG = "DatabaseHelper";
    private static final String DATABASE_NAME = "RFID_Demo.db";
    private static final int DATABASE_VERSION = 2;

    // Table names
    public static final String TABLE_USERS = "users";
    public static final String TABLE_PRODUCTS = "products";
    public static final String TABLE_SCAN_LOGS = "scan_logs";

    // Users table columns
    public static final String COLUMN_USER_ID = "user_id";
    public static final String COLUMN_USERNAME = "username";
    public static final String COLUMN_ROLE = "role";
    public static final String COLUMN_PASSWORD = "password";

    // Products table columns
    public static final String COLUMN_PRODUCT_ID = "product_id";
    public static final String COLUMN_TAG_ID = "tag_id";
    public static final String COLUMN_PRODUCT_NAME = "product_name";
    public static final String COLUMN_CATEGORY = "category";
    public static final String COLUMN_SIZE = "size";
    public static final String COLUMN_COLOR = "color";

    // Scan logs table columns
    public static final String COLUMN_LOG_ID = "log_id";
    public static final String COLUMN_SCAN_TIME = "scan_time";
    public static final String COLUMN_LOCATION = "location";
    public static final String COLUMN_STATUS = "status";
    public static final String COLUMN_IS_UPLOADED = "is_uploaded";

    // Create Users table
    private static final String CREATE_TABLE_USERS = 
        "CREATE TABLE " + TABLE_USERS + " (" +
        COLUMN_USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
        COLUMN_USERNAME + " TEXT UNIQUE NOT NULL, " +
        COLUMN_ROLE + " TEXT, " +
        COLUMN_PASSWORD + " TEXT NOT NULL" +
        ")";

    // Create Products table
    private static final String CREATE_TABLE_PRODUCTS = 
        "CREATE TABLE " + TABLE_PRODUCTS + " (" +
        COLUMN_PRODUCT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
        COLUMN_TAG_ID + " TEXT UNIQUE NOT NULL, " +
        COLUMN_PRODUCT_NAME + " TEXT, " +
        COLUMN_CATEGORY + " TEXT, " +
        COLUMN_SIZE + " TEXT, " +
        COLUMN_COLOR + " TEXT" +
        ")";

    // Create Scan Logs table
    private static final String CREATE_TABLE_SCAN_LOGS = 
        "CREATE TABLE " + TABLE_SCAN_LOGS + " (" +
        COLUMN_LOG_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
        COLUMN_TAG_ID + " TEXT, " +
        COLUMN_SCAN_TIME + " DATETIME, " +
        COLUMN_LOCATION + " TEXT, " +
        COLUMN_STATUS + " TEXT, " +
        COLUMN_IS_UPLOADED + " INTEGER DEFAULT 0, " +
        "FOREIGN KEY(" + COLUMN_TAG_ID + ") REFERENCES " + TABLE_PRODUCTS + "(" + COLUMN_TAG_ID + ")" +
        ")";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d(TAG, "Creating database tables");
        db.execSQL(CREATE_TABLE_USERS);
        db.execSQL(CREATE_TABLE_PRODUCTS);
        db.execSQL(CREATE_TABLE_SCAN_LOGS);
        
        // Insert default users
        insertDefaultUsers(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SCAN_LOGS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PRODUCTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        onCreate(db);
    }

    private void insertDefaultUsers(SQLiteDatabase db) {
        Log.d(TAG, "Inserting default users");
        ContentValues values = new ContentValues();
        
        // Insert default admin user
        values.put(COLUMN_USERNAME, "admin");
        values.put(COLUMN_ROLE, "Administrator");
        values.put(COLUMN_PASSWORD, "1234");
        db.insert(TABLE_USERS, null, values);
        
        // Insert default user
        values.clear();
        values.put(COLUMN_USERNAME, "user");
        values.put(COLUMN_ROLE, "User");
        values.put(COLUMN_PASSWORD, "1234");
        db.insert(TABLE_USERS, null, values);
    }

    // User methods
    public boolean validateUser(String username, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        String[] columns = {COLUMN_USER_ID};
        String selection = COLUMN_USERNAME + " = ? AND " + COLUMN_PASSWORD + " = ?";
        String[] selectionArgs = {username, password};
        
        Cursor cursor = db.query(TABLE_USERS, columns, selection, selectionArgs, null, null, null);
        boolean isValid = cursor.getCount() > 0;
        cursor.close();
        db.close();
        return isValid;
    }

    public User getUserByUsername(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        String[] columns = {COLUMN_USER_ID, COLUMN_USERNAME, COLUMN_ROLE};
        String selection = COLUMN_USERNAME + " = ?";
        String[] selectionArgs = {username};
        
        Cursor cursor = db.query(TABLE_USERS, columns, selection, selectionArgs, null, null, null);
        
        User user = null;
        if (cursor.moveToFirst()) {
            int userId = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_USER_ID));
            String userRole = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ROLE));
            user = new User(userId, username, userRole);
        }
        cursor.close();
        db.close();
        return user;
    }

    public void saveLoggedInUser(int userId) {
        // This would typically be saved in SharedPreferences
        // Implementation will be in the LoginActivity
    }

    public void clearLoggedInUser() {
        // This would typically clear the saved user from SharedPreferences
        // Implementation will be in the LoginActivity
    }
    
    // Bulk insert methods for sync
    public void insertUsersFromApi(List<User> users) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            // Clear existing users before sync (or use MERGE logic)
            db.delete(TABLE_USERS, null, null);
            
            for (User user : users) {
                ContentValues values = new ContentValues();
                values.put(COLUMN_USER_ID, user.getUserId());
                values.put(COLUMN_USERNAME, user.getUsername());
                values.put(COLUMN_ROLE, user.getRole());
                values.put(COLUMN_PASSWORD, "temp"); // API may not send password
                db.insertWithOnConflict(TABLE_USERS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            db.close();
        }
    }
    
    public void insertUserIfNotExists(User user) {
        SQLiteDatabase db = this.getWritableDatabase();
        
        // Check if user already exists
        String[] columns = {COLUMN_USER_ID};
        String selection = COLUMN_USERNAME + " = ?";
        String[] selectionArgs = {user.getUsername()};
        
        Cursor cursor = db.query(TABLE_USERS, columns, selection, selectionArgs, null, null, null);
        boolean userExists = cursor.getCount() > 0;
        cursor.close();
        
        if (!userExists) {
            ContentValues values = new ContentValues();
            values.put(COLUMN_USER_ID, user.getUserId());
            values.put(COLUMN_USERNAME, user.getUsername());
            values.put(COLUMN_ROLE, user.getRole());
            values.put(COLUMN_PASSWORD, "local"); // Default for local user
            db.insert(TABLE_USERS, null, values);
        }
        db.close();
    }
    
    public void insertProductsFromApi(List<Product> products) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            // Clear existing products before sync (or use MERGE logic)
            db.delete(TABLE_PRODUCTS, null, null);
            
            for (Product product : products) {
                ContentValues values = new ContentValues();
                values.put(COLUMN_TAG_ID, product.getTagId());
                values.put(COLUMN_PRODUCT_NAME, product.getProductName());
                values.put(COLUMN_CATEGORY, product.getCategory());
                values.put(COLUMN_SIZE, product.getSize());
                values.put(COLUMN_COLOR, product.getColor());
                db.insertWithOnConflict(TABLE_PRODUCTS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            db.close();
        }
    }
    
    public void insertScanLogsFromApi(List<ScanLog> scanLogs) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            // Clear existing scan logs before sync (or use MERGE logic)
            db.delete(TABLE_SCAN_LOGS, null, null);
            
            for (ScanLog scanLog : scanLogs) {
                ContentValues values = new ContentValues();
                values.put(COLUMN_TAG_ID, scanLog.getTagId());
                values.put(COLUMN_SCAN_TIME, scanLog.getScanTime());
                values.put(COLUMN_LOCATION, scanLog.getLocation());
                values.put(COLUMN_STATUS, scanLog.getStatus());
                db.insertWithOnConflict(TABLE_SCAN_LOGS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            db.close();
        }
    }
    
    // Methods for data upload service
    public List<Product> getLocalProductsForUpload() {
        List<Product> products = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        
        // Query for products that need to be uploaded (e.g., those not marked as synced)
        String[] columns = {COLUMN_PRODUCT_ID, COLUMN_TAG_ID, COLUMN_PRODUCT_NAME, 
                           COLUMN_CATEGORY, COLUMN_SIZE, COLUMN_COLOR};
        
        Cursor cursor = db.query(TABLE_PRODUCTS, columns, null, null, null, null, null);
        
        while (cursor.moveToNext()) {
            int productId = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PRODUCT_ID));
            String tagId = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TAG_ID));
            String productName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PRODUCT_NAME));
            String category = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CATEGORY));
            String size = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SIZE));
            String color = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_COLOR));
            
            products.add(new Product(productId, tagId, productName, category, size, color));
        }
        cursor.close();
        db.close();
        return products;
    }
    
    public List<ScanLog> getLocalScanLogsForUpload() {
        List<ScanLog> scanLogs = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        
        // Query for scan logs that need to be uploaded (is_uploaded = 0)
        String[] columns = {COLUMN_LOG_ID, COLUMN_TAG_ID, COLUMN_SCAN_TIME, 
                           COLUMN_LOCATION, COLUMN_STATUS};
        String selection = COLUMN_IS_UPLOADED + " = 0";
        
        Cursor cursor = db.query(TABLE_SCAN_LOGS, columns, selection, null, null, null, null);
        
        while (cursor.moveToNext()) {
            int logId = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_LOG_ID));
            String tagId = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TAG_ID));
            String scanTime = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SCAN_TIME));
            String location = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LOCATION));
            String status = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_STATUS));
            
            scanLogs.add(new ScanLog(logId, tagId, scanTime, location, status));
        }
        cursor.close();
        db.close();
        return scanLogs;
    }
    
    public void markProductAsUploaded(String tagId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        // In a real implementation, you might have an 'uploaded' column
        // For now, we'll just ensure the product exists in the database
        db.update(TABLE_PRODUCTS, values, COLUMN_TAG_ID + " = ?", new String[]{tagId});
        db.close();
    }
    
    public void markScanLogAsUploaded(int logId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_IS_UPLOADED, 1);
        db.update(TABLE_SCAN_LOGS, values, COLUMN_LOG_ID + " = ?", new String[]{String.valueOf(logId)});
        db.close();
    }
    
    // Product methods
    public long insertProduct(Product product) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TAG_ID, product.getTagId());
        values.put(COLUMN_PRODUCT_NAME, product.getProductName());
        values.put(COLUMN_CATEGORY, product.getCategory());
        values.put(COLUMN_SIZE, product.getSize());
        values.put(COLUMN_COLOR, product.getColor());
        
        long id = db.insert(TABLE_PRODUCTS, null, values);
        db.close();
        return id;
    }
    
    public List<Product> getAllProducts() {
        List<Product> productList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String[] columns = {COLUMN_PRODUCT_ID, COLUMN_TAG_ID, COLUMN_PRODUCT_NAME, 
                           COLUMN_CATEGORY, COLUMN_SIZE, COLUMN_COLOR};
        
        Cursor cursor = db.query(TABLE_PRODUCTS, columns, null, null, null, null, null);
        
        while (cursor.moveToNext()) {
            int productId = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PRODUCT_ID));
            String tagId = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TAG_ID));
            String productName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PRODUCT_NAME));
            String category = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CATEGORY));
            String size = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SIZE));
            String color = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_COLOR));
            
            productList.add(new Product(productId, tagId, productName, category, size, color));
        }
        cursor.close();
        db.close();
        return productList;
    }
    
    // Check if a tag ID exists in the products table
    public boolean isTagIdInProducts(String tagId) {
        SQLiteDatabase db = this.getReadableDatabase();
        String[] columns = {COLUMN_TAG_ID};
        String selection = COLUMN_TAG_ID + " = ?";
        String[] selectionArgs = {tagId};
        
        Cursor cursor = db.query(TABLE_PRODUCTS, columns, selection, selectionArgs, null, null, null);
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        db.close();
        return exists;
    }
    
    // Scan log methods
    public long insertScanLog(ScanLog scanLog) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TAG_ID, scanLog.getTagId());
        values.put(COLUMN_SCAN_TIME, scanLog.getScanTime());
        values.put(COLUMN_LOCATION, scanLog.getLocation());
        values.put(COLUMN_STATUS, scanLog.getStatus());
        values.put(COLUMN_IS_UPLOADED, 0); // Default to not uploaded
        
        long id = db.insert(TABLE_SCAN_LOGS, null, values);
        db.close();
        return id;
    }
    
    public List<ScanLog> getAllScanLogs() {
        List<ScanLog> scanLogList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String[] columns = {COLUMN_LOG_ID, COLUMN_TAG_ID, COLUMN_SCAN_TIME, 
                           COLUMN_LOCATION, COLUMN_STATUS};
        
        Cursor cursor = db.query(TABLE_SCAN_LOGS, columns, null, null, null, null, 
                               COLUMN_SCAN_TIME + " DESC");
        
        while (cursor.moveToNext()) {
            int logId = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_LOG_ID));
            String tagId = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TAG_ID));
            String scanTime = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SCAN_TIME));
            String location = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LOCATION));
            String status = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_STATUS));
            
            scanLogList.add(new ScanLog(logId, tagId, scanTime, location, status));
        }
        cursor.close();
        db.close();
        return scanLogList;
    }
    
    // Count methods for dashboard
    public int getTotalScanLogCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_SCAN_LOGS, null);
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        db.close();
        return count;
    }
    
    public int getTodayScanLogCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        // Get today's date in YYYY-MM-DD format
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
        String today = sdf.format(new java.util.Date());
        
        // Query for scan logs from today
        String query = "SELECT COUNT(*) FROM " + TABLE_SCAN_LOGS + 
                      " WHERE DATE(" + COLUMN_SCAN_TIME + ") = ?";
        Cursor cursor = db.rawQuery(query, new String[]{today});
        
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        db.close();
        return count;
    }
    
    public List<String> getDistinctScanDates() {
        List<String> dates = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        
        // Query for distinct dates from scan logs
        String query = "SELECT DISTINCT DATE(" + COLUMN_SCAN_TIME + ") AS scan_date FROM " + 
                      TABLE_SCAN_LOGS + " ORDER BY scan_date DESC";
        Cursor cursor = db.rawQuery(query, null);
        
        while (cursor.moveToNext()) {
            String date = cursor.getString(0);
            if (date != null) {
                dates.add(date);
            }
        }
        cursor.close();
        db.close();
        return dates;
    }
    
    public List<ScanLog> getFilteredScanLogs(String date, String status) {
        List<ScanLog> scanLogs = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("SELECT * FROM " + TABLE_SCAN_LOGS);
        
        List<String> selectionArgs = new ArrayList<>();
        boolean hasWhereClause = false;
        
        // Add date filter if provided
        if (date != null && !date.isEmpty() && !date.equals("No data available")) {
            queryBuilder.append(hasWhereClause ? " AND " : " WHERE ");
            queryBuilder.append("DATE(" + COLUMN_SCAN_TIME + ") = ?");
            selectionArgs.add(date);
            hasWhereClause = true;
        }
        
        // Add status filter if provided
        if (status != null && !status.isEmpty()) {
            queryBuilder.append(hasWhereClause ? " AND " : " WHERE ");
            queryBuilder.append("UPPER(" + COLUMN_STATUS + ") = UPPER(?)");
            selectionArgs.add(status);
            hasWhereClause = true;
        }
        
        queryBuilder.append(" ORDER BY " + COLUMN_SCAN_TIME + " DESC");
        
        Cursor cursor = db.rawQuery(queryBuilder.toString(), selectionArgs.toArray(new String[0]));
        
        while (cursor.moveToNext()) {
            int logId = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_LOG_ID));
            String tagId = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TAG_ID));
            String scanTime = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SCAN_TIME));
            String location = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LOCATION));
            String dbStatus = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_STATUS));
            
            scanLogs.add(new ScanLog(logId, tagId, scanTime, location, dbStatus));
        }
        
        cursor.close();
        db.close();
        
        return scanLogs;
    }
}