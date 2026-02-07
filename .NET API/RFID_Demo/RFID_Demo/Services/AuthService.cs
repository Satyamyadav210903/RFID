using Microsoft.IdentityModel.Tokens;
using RFID_Demo.Models;
using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;
using System.Text;

namespace RFID_Demo.Services
{
    public class AuthService : IAuthService
    {
        private readonly IConfiguration _configuration;

        public AuthService(IConfiguration configuration)
        {
            _configuration = configuration;
        }

        public async Task<TokenResponse> GenerateTokenAsync(string username)
        {
            var tokenHandler = new JwtSecurityTokenHandler();
            var key = Encoding.ASCII.GetBytes(_configuration["Jwt:Key"]);
            
            var tokenDescriptor = new SecurityTokenDescriptor
            {
                Subject = new ClaimsIdentity(new[]
                {
                    new Claim(ClaimTypes.Name, username),
                    new Claim(JwtRegisteredClaimNames.Jti, Guid.NewGuid().ToString())
                }),
                Expires = DateTime.UtcNow.AddMinutes(Convert.ToInt32(_configuration["Jwt:DurationInMinutes"] ?? "60")),
                Issuer = _configuration["Jwt:Issuer"] ?? "RFID_API",
                Audience = _configuration["Jwt:Audience"] ?? "RFID_Client",
                SigningCredentials = new SigningCredentials(new SymmetricSecurityKey(key), SecurityAlgorithms.HmacSha256Signature)
            };

            var token = tokenHandler.CreateToken(tokenDescriptor);
            var tokenString = tokenHandler.WriteToken(token);

            return new TokenResponse
            {
                Token = tokenString,
                Expiration = tokenDescriptor.Expires ?? DateTime.UtcNow.AddMinutes(60)
            };
        }

        public bool ValidatePassword(string inputPassword, string storedPassword)
        {
            // Simple password comparison - in production, use proper hashing
            return !string.IsNullOrEmpty(inputPassword) && 
                   !string.IsNullOrEmpty(storedPassword) && 
                   inputPassword.Equals(storedPassword, StringComparison.Ordinal);
        }
    }
}