using RFID_Demo.Models;

namespace RFID_Demo.Data
{
    public interface IRFIDRepository
    {
        // User methods
        Task<User?> GetUserByUsernameAsync(string username);
        Task<bool> CreateUserAsync(User user);
        
        // Product methods
        Task<IEnumerable<Product>> GetAllProductsAsync();
        Task<Product?> GetProductByTagIdAsync(string tagId);
        Task<bool> CreateProductAsync(Product product);
        Task<bool> UpdateProductAsync(Product product);
        Task<bool> DeleteProductAsync(int productId);
        
        // Scan log methods
        Task<IEnumerable<ScanLog>> GetAllScanLogsAsync();
        Task<IEnumerable<ScanLog>> GetScanLogsByTagIdAsync(string tagId);
        Task<bool> CreateScanLogAsync(ScanLog scanLog);
        Task<bool> UpdateScanLogAsync(ScanLog scanLog);
        Task<bool> DeleteScanLogAsync(int logId);
    }
}