using RFID_Demo.Models;
using System.Data;
using System.Data.SqlClient;

namespace RFID_Demo.Data
{
    public class RFIDRepository : IRFIDRepository
    {
        private readonly IDbConnection _connection;
        private readonly IConfiguration _configuration;

        public RFIDRepository(IDbConnection connection, IConfiguration configuration)
        {
            _connection = connection;
            _configuration = configuration;
        }

        // User methods
        public async Task<User?> GetUserByUsernameAsync(string username)
        {
            using var conn = new SqlConnection(_connection.ConnectionString);
            await conn.OpenAsync();

            var sql = "SELECT user_id, username, role, password FROM users WHERE username = @Username";
            using var cmd = new SqlCommand(sql, conn);
            cmd.Parameters.AddWithValue("@Username", username);

            using var reader = await cmd.ExecuteReaderAsync();
            if (await reader.ReadAsync())
            {
                return new User
                {
                    UserId = reader.GetInt32("user_id"),
                    Username = reader.GetString("username"),
                    Role = reader.GetString("role"),
                    Password = reader.IsDBNull("password") ? string.Empty : reader.GetString("password")
                };
            }

            return null;
        }

        public async Task<bool> CreateUserAsync(User user)
        {
            using var conn = new SqlConnection(_connection.ConnectionString);
            await conn.OpenAsync();

            var sql = "INSERT INTO users (username, role, password) VALUES (@Username, @Role, @Password)";
            using var cmd = new SqlCommand(sql, conn);
            cmd.Parameters.AddWithValue("@Username", user.Username);
            cmd.Parameters.AddWithValue("@Role", user.Role);
            cmd.Parameters.AddWithValue("@Password", user.Password);

            var rowsAffected = await cmd.ExecuteNonQueryAsync();
            return rowsAffected > 0;
        }

        // Product methods
        public async Task<IEnumerable<Product>> GetAllProductsAsync()
        {
            var products = new List<Product>();
            using var conn = new SqlConnection(_connection.ConnectionString);
            await conn.OpenAsync();

            var sql = "SELECT product_id, tag_id, product_name, category, size, color FROM products ORDER BY product_name";
            using var cmd = new SqlCommand(sql, conn);

            using var reader = await cmd.ExecuteReaderAsync();
            while (await reader.ReadAsync())
            {
                products.Add(new Product
                {
                    ProductId = reader.GetInt32("product_id"),
                    TagId = reader.GetString("tag_id"),
                    ProductName = reader.GetString("product_name"),
                    Category = reader.IsDBNull("category") ? string.Empty : reader.GetString("category"),
                    Size = reader.IsDBNull("size") ? string.Empty : reader.GetString("size"),
                    Color = reader.IsDBNull("color") ? string.Empty : reader.GetString("color")
                });
            }

            return products;
        }

        public async Task<Product?> GetProductByTagIdAsync(string tagId)
        {
            using var conn = new SqlConnection(_connection.ConnectionString);
            await conn.OpenAsync();

            var sql = "SELECT product_id, tag_id, product_name, category, size, color FROM products WHERE tag_id = @TagId";
            using var cmd = new SqlCommand(sql, conn);
            cmd.Parameters.AddWithValue("@TagId", tagId);

            using var reader = await cmd.ExecuteReaderAsync();
            if (await reader.ReadAsync())
            {
                return new Product
                {
                    ProductId = reader.GetInt32("product_id"),
                    TagId = reader.GetString("tag_id"),
                    ProductName = reader.GetString("product_name"),
                    Category = reader.IsDBNull("category") ? string.Empty : reader.GetString("category"),
                    Size = reader.IsDBNull("size") ? string.Empty : reader.GetString("size"),
                    Color = reader.IsDBNull("color") ? string.Empty : reader.GetString("color")
                };
            }

            return null;
        }

        public async Task<bool> CreateProductAsync(Product product)
        {
            using var conn = new SqlConnection(_connection.ConnectionString);
            await conn.OpenAsync();

            var sql = "INSERT INTO products (tag_id, product_name, category, size, color) VALUES (@TagId, @ProductName, @Category, @Size, @Color)";
            using var cmd = new SqlCommand(sql, conn);
            cmd.Parameters.AddWithValue("@TagId", product.TagId);
            cmd.Parameters.AddWithValue("@ProductName", product.ProductName);
            cmd.Parameters.AddWithValue("@Category", product.Category ?? (object)DBNull.Value);
            cmd.Parameters.AddWithValue("@Size", product.Size ?? (object)DBNull.Value);
            cmd.Parameters.AddWithValue("@Color", product.Color ?? (object)DBNull.Value);

            var rowsAffected = await cmd.ExecuteNonQueryAsync();
            return rowsAffected > 0;
        }

        public async Task<bool> UpdateProductAsync(Product product)
        {
            using var conn = new SqlConnection(_connection.ConnectionString);
            await conn.OpenAsync();

            var sql = "UPDATE products SET product_name = @ProductName, category = @Category, size = @Size, color = @Color WHERE product_id = @ProductId";
            using var cmd = new SqlCommand(sql, conn);
            cmd.Parameters.AddWithValue("@ProductId", product.ProductId);
            cmd.Parameters.AddWithValue("@ProductName", product.ProductName);
            cmd.Parameters.AddWithValue("@Category", product.Category ?? (object)DBNull.Value);
            cmd.Parameters.AddWithValue("@Size", product.Size ?? (object)DBNull.Value);
            cmd.Parameters.AddWithValue("@Color", product.Color ?? (object)DBNull.Value);

            var rowsAffected = await cmd.ExecuteNonQueryAsync();
            return rowsAffected > 0;
        }

        public async Task<bool> DeleteProductAsync(int productId)
        {
            using var conn = new SqlConnection(_connection.ConnectionString);
            await conn.OpenAsync();

            var sql = "DELETE FROM products WHERE product_id = @ProductId";
            using var cmd = new SqlCommand(sql, conn);
            cmd.Parameters.AddWithValue("@ProductId", productId);

            var rowsAffected = await cmd.ExecuteNonQueryAsync();
            return rowsAffected > 0;
        }

        // Scan log methods
        public async Task<IEnumerable<ScanLog>> GetAllScanLogsAsync()
        {
            var logs = new List<ScanLog>();
            using var conn = new SqlConnection(_connection.ConnectionString);
            await conn.OpenAsync();

            var sql = "SELECT log_id, tag_id, scan_time, location, status FROM scan_logs ORDER BY scan_time DESC";
            using var cmd = new SqlCommand(sql, conn);

            using var reader = await cmd.ExecuteReaderAsync();
            while (await reader.ReadAsync())
            {
                logs.Add(new ScanLog
                {
                    LogId = reader.GetInt32("log_id"),
                    TagId = reader.GetString("tag_id"),
                    ScanTime = reader.GetDateTime("scan_time"),
                    Location = reader.IsDBNull("location") ? string.Empty : reader.GetString("location"),
                    Status = reader.IsDBNull("status") ? string.Empty : reader.GetString("status")
                });
            }

            return logs;
        }

        public async Task<IEnumerable<ScanLog>> GetScanLogsByTagIdAsync(string tagId)
        {
            var logs = new List<ScanLog>();
            using var conn = new SqlConnection(_connection.ConnectionString);
            await conn.OpenAsync();

            var sql = "SELECT log_id, tag_id, scan_time, location, status FROM scan_logs WHERE tag_id = @TagId ORDER BY scan_time DESC";
            using var cmd = new SqlCommand(sql, conn);
            cmd.Parameters.AddWithValue("@TagId", tagId);

            using var reader = await cmd.ExecuteReaderAsync();
            while (await reader.ReadAsync())
            {
                logs.Add(new ScanLog
                {
                    LogId = reader.GetInt32("log_id"),
                    TagId = reader.GetString("tag_id"),
                    ScanTime = reader.GetDateTime("scan_time"),
                    Location = reader.IsDBNull("location") ? string.Empty : reader.GetString("location"),
                    Status = reader.IsDBNull("status") ? string.Empty : reader.GetString("status")
                });
            }

            return logs;
        }

        //public async Task<bool> CreateScanLogAsync(ScanLog scanLog)
        //{
        //    using var conn = new SqlConnection(_connection.ConnectionString);
        //    await conn.OpenAsync();

        //    var sql = "INSERT INTO scan_logs (tag_id, scan_time, location, status) VALUES (@TagId, @ScanTime, @Location, @Status)";
        //    using var cmd = new SqlCommand(sql, conn);
        //    cmd.Parameters.AddWithValue("@TagId", scanLog.TagId);
        //    cmd.Parameters.AddWithValue("@ScanTime", scanLog.ScanTime);
        //    cmd.Parameters.AddWithValue("@Location", scanLog.Location ?? (object)DBNull.Value);
        //    cmd.Parameters.AddWithValue("@Status", scanLog.Status ?? (object)DBNull.Value);

        //    var rowsAffected = await cmd.ExecuteNonQueryAsync();
        //    return rowsAffected > 0;
        //}

        public async Task<bool> CreateScanLogAsync(ScanLog scanLog)
        {
            using var conn = new SqlConnection(_connection.ConnectionString);
            await conn.OpenAsync();

            //Check if tag already exists
            var checkSql = "SELECT COUNT(1) FROM scan_logs WHERE tag_id = @TagId AND scan_time = @ScanTime";
            using (var checkCmd = new SqlCommand(checkSql, conn))
            {
                checkCmd.Parameters.AddWithValue("@TagId", scanLog.TagId);
                checkCmd.Parameters.AddWithValue("@ScanTime", scanLog.ScanTime);
                var exists = (int)await checkCmd.ExecuteScalarAsync() > 0;

                if (exists)
                {
                    // Skip insert
                    return false;
                }
            }

            // Insert if not exists
            var insertSql = @"
        INSERT INTO scan_logs (tag_id, scan_time, location, status)
        VALUES (@TagId, @ScanTime, @Location, @Status)";

            using var insertCmd = new SqlCommand(insertSql, conn);
            insertCmd.Parameters.AddWithValue("@TagId", scanLog.TagId);
            insertCmd.Parameters.AddWithValue("@ScanTime", scanLog.ScanTime);
            insertCmd.Parameters.AddWithValue("@Location", scanLog.Location ?? (object)DBNull.Value);
            insertCmd.Parameters.AddWithValue("@Status", scanLog.Status ?? (object)DBNull.Value);

            var rowsAffected = await insertCmd.ExecuteNonQueryAsync();
            return rowsAffected > 0;
        }







        public async Task<bool> UpdateScanLogAsync(ScanLog scanLog)
        {
            using var conn = new SqlConnection(_connection.ConnectionString);
            await conn.OpenAsync();

            var sql = "UPDATE scan_logs SET scan_time = @ScanTime, location = @Location, status = @Status WHERE log_id = @LogId";
            using var cmd = new SqlCommand(sql, conn);
            cmd.Parameters.AddWithValue("@LogId", scanLog.LogId);
            cmd.Parameters.AddWithValue("@ScanTime", scanLog.ScanTime);
            cmd.Parameters.AddWithValue("@Location", scanLog.Location ?? (object)DBNull.Value);
            cmd.Parameters.AddWithValue("@Status", scanLog.Status ?? (object)DBNull.Value);

            var rowsAffected = await cmd.ExecuteNonQueryAsync();
            return rowsAffected > 0;
        }

        public async Task<bool> DeleteScanLogAsync(int logId)
        {
            using var conn = new SqlConnection(_connection.ConnectionString);
            await conn.OpenAsync();

            var sql = "DELETE FROM scan_logs WHERE log_id = @LogId";
            using var cmd = new SqlCommand(sql, conn);
            cmd.Parameters.AddWithValue("@LogId", logId);

            var rowsAffected = await cmd.ExecuteNonQueryAsync();
            return rowsAffected > 0;
        }


    }
}