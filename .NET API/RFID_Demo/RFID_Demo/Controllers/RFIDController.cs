using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using RFID_Demo.Data;
using RFID_Demo.Models;

namespace RFID_Demo.Controllers
{
    [ApiController]
    [Route("api/[controller]")]
    [Authorize] // Require authentication for all endpoints in this controller
    public class RFIDController : ControllerBase
    {
        private readonly IRFIDRepository _repository;

        public RFIDController(IRFIDRepository repository)
        {
            _repository = repository;
        }

        #region Product Operations
        
        [HttpGet("products")]
        public async Task<ActionResult<IEnumerable<Product>>> GetAllProducts()
        {
            var products = await _repository.GetAllProductsAsync();
            return Ok(products);
        }

        [HttpGet("products/{tagId}")]
        public async Task<ActionResult<Product>> GetProductByTagId(string tagId)
        {
            var product = await _repository.GetProductByTagIdAsync(tagId);
            if (product == null)
            {
                return NotFound($"Product with tag ID '{tagId}' not found");
            }
            return Ok(product);
        }

        [HttpPost("products")]
        public async Task<ActionResult> CreateProduct([FromBody] Product product)
        {
            if (!ModelState.IsValid)
            {
                return BadRequest(ModelState);
            }

            // Check if product with this tag ID already exists
            var existingProduct = await _repository.GetProductByTagIdAsync(product.TagId);
            if (existingProduct != null)
            {
                return BadRequest($"Product with tag ID '{product.TagId}' already exists");
            }

            var result = await _repository.CreateProductAsync(product);
            if (result)
            {
                return CreatedAtAction(nameof(GetProductByTagId), new { tagId = product.TagId }, product);
            }

            return StatusCode(500, "Failed to create product");
        }

        [HttpPut("products/{id}")]
        public async Task<ActionResult> UpdateProduct(int id, [FromBody] Product product)
        {
            if (!ModelState.IsValid)
            {
                return BadRequest(ModelState);
            }

            if (id != product.ProductId)
            {
                return BadRequest("Product ID mismatch");
            }

            var result = await _repository.UpdateProductAsync(product);
            if (result)
            {
                return Ok(product);
            }

            return StatusCode(500, "Failed to update product");
        }

        [HttpDelete("products/{id}")]
        public async Task<ActionResult> DeleteProduct(int id)
        {
            var result = await _repository.DeleteProductAsync(id);
            if (result)
            {
                return Ok(new { message = "Product deleted successfully" });
            }

            return StatusCode(500, "Failed to delete product");
        }

        #endregion

        #region Scan Log Operations

        [HttpGet("scanlogs")]
        public async Task<ActionResult<IEnumerable<ScanLog>>> GetAllScanLogs()
        {
            var logs = await _repository.GetAllScanLogsAsync();
            return Ok(logs);
        }

        [HttpGet("scanlogs/{tagId}")]
        public async Task<ActionResult<IEnumerable<ScanLog>>> GetScanLogsByTagId(string tagId)
        {
            var logs = await _repository.GetScanLogsByTagIdAsync(tagId);
            return Ok(logs);
        }

        [HttpPost("scanlogs")]
        public async Task<ActionResult> CreateScanLog([FromBody] ScanLog scanLog)
        {
            if (!ModelState.IsValid)
            {
                return BadRequest(ModelState);
            }

            // Verify that the product exists before creating a scan log
            var product = await _repository.GetProductByTagIdAsync(scanLog.TagId);
            if (product == null)
            {
                return BadRequest($"Product with tag ID '{scanLog.TagId}' does not exist");
            }

            // Set the scan time to now if not provided
            if (scanLog.ScanTime == default(DateTime))
            {
                scanLog.ScanTime = DateTime.UtcNow;
            }

            var result = await _repository.CreateScanLogAsync(scanLog);
            if (result)
            {
                return CreatedAtAction(nameof(GetScanLogsByTagId), new { tagId = scanLog.TagId }, scanLog);
            }

            return StatusCode(500, "Failed to create scan log");
        }

        [HttpPut("scanlogs/{id}")]
        public async Task<ActionResult> UpdateScanLog(int id, [FromBody] ScanLog scanLog)
        {
            if (!ModelState.IsValid)
            {
                return BadRequest(ModelState);
            }

            if (id != scanLog.LogId)
            {
                return BadRequest("Scan log ID mismatch");
            }

            var result = await _repository.UpdateScanLogAsync(scanLog);
            if (result)
            {
                return Ok(scanLog);
            }

            return StatusCode(500, "Failed to update scan log");
        }

        [HttpDelete("scanlogs/{id}")]
        public async Task<ActionResult> DeleteScanLog(int id)
        {
            var result = await _repository.DeleteScanLogAsync(id);
            if (result)
            {
                return Ok(new { message = "Scan log deleted successfully" });
            }

            return StatusCode(500, "Failed to delete scan log");
        }

        #endregion

        #region RFID-Specific Operations

        /// <summary>
        /// Simulates RFID scanning - creates a scan log entry for a given tag
        /// </summary>
        [HttpPost("scan")]
        public async Task<ActionResult> ScanRFID([FromBody] ScanLog scanData)
        {
            if (!ModelState.IsValid)
            {
                return BadRequest(ModelState);
            }

            // Verify that the product exists
            var product = await _repository.GetProductByTagIdAsync(scanData.TagId);
            if (product == null)
            {
                return BadRequest($"Product with tag ID '{scanData.TagId}' does not exist");
            }

            // Create a scan log entry
            var scanLog = new ScanLog
            {
                TagId = scanData.TagId,
                ScanTime = scanData.ScanTime == default(DateTime) ? DateTime.UtcNow : scanData.ScanTime,
                Location = scanData.Location,
                Status = scanData.Status
            };

            var result = await _repository.CreateScanLogAsync(scanLog);
            if (result)
            {
                return Ok(new { 
                    message = "RFID scan recorded successfully", 
                    tagId = scanLog.TagId,
                    scanTime = scanLog.ScanTime,
                    status = scanLog.Status 
                });
            }

            return StatusCode(500, "Failed to record RFID scan");
        }

        #endregion
    }
}