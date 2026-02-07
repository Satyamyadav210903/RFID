using System.ComponentModel.DataAnnotations;

namespace RFID_Demo.Models
{
    public class Product
    {
        public int ProductId { get; set; }
        
        [Required]
        public string TagId { get; set; } = string.Empty;
        
        [Required]
        public string ProductName { get; set; } = string.Empty;
        
        public string Category { get; set; } = string.Empty;
        
        public string Size { get; set; } = string.Empty;
        
        public string Color { get; set; } = string.Empty;
    }
}