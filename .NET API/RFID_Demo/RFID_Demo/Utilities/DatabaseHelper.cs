using System.Data;
using System.Data.SqlClient;

namespace RFID_Demo.Utilities
{
    public static class DatabaseHelper
    {
        public static async Task<bool> TestConnectionAsync(string connectionString)
        {
            try
            {
                using var connection = new SqlConnection(connectionString);
                await connection.OpenAsync();
                var command = new SqlCommand("SELECT 1", connection);
                var result = await command.ExecuteScalarAsync();
                return result != null;
            }
            catch
            {
                return false;
            }
        }
        
        public static async Task EnsureDatabaseExistsAsync(string connectionString)
        {
            // Extract database name from connection string
            var builder = new SqlConnectionStringBuilder(connectionString);
            var databaseName = builder.InitialCatalog;
            var serverName = builder.DataSource;
            
            // Create a connection string without the database name to connect to master
            builder.InitialCatalog = "master";
            var masterConnectionString = builder.ToString();
            
            using var connection = new SqlConnection(masterConnectionString);
            await connection.OpenAsync();
            
            // Check if database exists
            var checkDbQuery = $"SELECT database_id FROM sys.databases WHERE Name = '{databaseName}'";
            using var checkCommand = new SqlCommand(checkDbQuery, connection);
            var result = await checkCommand.ExecuteScalarAsync();
            
            if (result == null || result == DBNull.Value)
            {
                // Database doesn't exist, create it
                var createDbQuery = $"CREATE DATABASE [{databaseName}]";
                using var createCommand = new SqlCommand(createDbQuery, connection);
                await createCommand.ExecuteNonQueryAsync();
            }
        }
        
        public static async Task EnsureTablesExistAsync(string connectionString)
        {
            using var connection = new SqlConnection(connectionString);
            await connection.OpenAsync();
            
            // Check if users table exists
            var checkUsersTable = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'users'";
            using var usersCheckCmd = new SqlCommand(checkUsersTable, connection);
            var usersTableExists = (int)await usersCheckCmd.ExecuteScalarAsync() > 0;
            
            if (!usersTableExists)
            {
                // Create users table with password column
                var createUsersTable = @"CREATE TABLE users (
                    user_id INT IDENTITY(1,1) PRIMARY KEY,
                    username VARCHAR(50),
                    role VARCHAR(30),
                    password VARCHAR(50)
                );";
                using var createUserCmd = new SqlCommand(createUsersTable, connection);
                await createUserCmd.ExecuteNonQueryAsync();
            }
            
            // Check if products table exists
            var checkProductsTable = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'products'";
            using var productsCheckCmd = new SqlCommand(checkProductsTable, connection);
            var productsTableExists = (int)await productsCheckCmd.ExecuteScalarAsync() > 0;
            
            if (!productsTableExists)
            {
                // Create products table
                var createProductsTable = @"CREATE TABLE products (
                    product_id INT IDENTITY(1,1) PRIMARY KEY,
                    tag_id VARCHAR(50) UNIQUE NOT NULL,
                    product_name VARCHAR(100),
                    category VARCHAR(50),
                    size VARCHAR(10),
                    color VARCHAR(30)
                );";
                using var createProductCmd = new SqlCommand(createProductsTable, connection);
                await createProductCmd.ExecuteNonQueryAsync();
                
                // Insert sample product
                var insertSampleProduct = @"INSERT INTO products (tag_id, product_name, category, size, color)
                VALUES ('300833B2DDD9014000000001', 'Cotton T-Shirt', 'T-Shirt', 'M', 'Black');";
                using var insertProductCmd = new SqlCommand(insertSampleProduct, connection);
                await insertProductCmd.ExecuteNonQueryAsync();
            }
            
            // Check if scan_logs table exists
            var checkScanLogsTable = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'scan_logs'";
            using var scanLogsCheckCmd = new SqlCommand(checkScanLogsTable, connection);
            var scanLogsTableExists = (int)await scanLogsCheckCmd.ExecuteScalarAsync() > 0;
            
            if (!scanLogsTableExists)
            {
                // Create scan_logs table
                var createScanLogsTable = @"CREATE TABLE scan_logs (
                    log_id INT IDENTITY(1,1) PRIMARY KEY,
                    tag_id VARCHAR(50),
                    scan_time DATETIME,
                    location VARCHAR(50),
                    status VARCHAR(20),
                    FOREIGN KEY (tag_id) REFERENCES products(tag_id)
                );";
                using var createScanLogCmd = new SqlCommand(createScanLogsTable, connection);
                await createScanLogCmd.ExecuteNonQueryAsync();
            }
        }
    }
}