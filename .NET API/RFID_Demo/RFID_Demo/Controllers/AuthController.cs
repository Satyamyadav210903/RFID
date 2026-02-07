using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using RFID_Demo.Data;
using RFID_Demo.Models;
using RFID_Demo.Services;

namespace RFID_Demo.Controllers
{
    [ApiController]
    [Route("api/[controller]")]
    public class AuthController : ControllerBase
    {
        private readonly IRFIDRepository _repository;
        private readonly IAuthService _authService;

        public AuthController(IRFIDRepository repository, IAuthService authService)
        {
            _repository = repository;
            _authService = authService;
        }

        [HttpPost("login")]
        public async Task<ActionResult<TokenResponse>> Login([FromBody] LoginModel model)
        {
            if (!ModelState.IsValid)
            {
                return BadRequest(ModelState);
            }

            if (string.IsNullOrEmpty(model.Username) || string.IsNullOrEmpty(model.Password))
            {
                return Unauthorized(new { message = "Username and password are required" });
            }

            // Check if user exists in database
            var user = await _repository.GetUserByUsernameAsync(model.Username);
            if (user == null)
            {
                return Unauthorized(new { message = "Invalid username or password" });
            }

            // Validate password
            if (!_authService.ValidatePassword(model.Password, user.Password))
            {
                return Unauthorized(new { message = "Invalid username or password" });
            }

            var token = await _authService.GenerateTokenAsync(user.Username);
            return Ok(token);
        }

        [HttpPost("register")]
        public async Task<ActionResult> Register([FromBody] User userModel)
        {
            if (!ModelState.IsValid)
            {
                return BadRequest(ModelState);
            }

            // Check if user already exists
            var existingUser = await _repository.GetUserByUsernameAsync(userModel.Username);
            if (existingUser != null)
            {
                return BadRequest(new { message = "Username already exists" });
            }

            // Create new user with password
            var newUser = new User
            {
                Username = userModel.Username,
                Role = userModel.Role,
                Password = userModel.Password
            };

            var result = await _repository.CreateUserAsync(newUser);
            if (result)
            {
                return Ok(new { message = "User created successfully" });
            }

            return StatusCode(500, new { message = "Failed to create user" });
        }


    }
}