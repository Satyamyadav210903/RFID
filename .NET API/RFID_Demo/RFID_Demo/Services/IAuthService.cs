using RFID_Demo.Models;

namespace RFID_Demo.Services
{
    public interface IAuthService
    {
        Task<TokenResponse> GenerateTokenAsync(string username);
        bool ValidatePassword(string password, string hash);
    }
}