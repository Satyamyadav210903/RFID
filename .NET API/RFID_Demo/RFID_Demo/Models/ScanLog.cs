using System.ComponentModel.DataAnnotations;

namespace RFID_Demo.Models
{
    public class ScanLog
    {
        public int LogId { get; set; }
        
        [Required]
        public string TagId { get; set; } = string.Empty;
        
        public DateTime ScanTime { get; set; } = DateTime.UtcNow;
        
        public string Location { get; set; } = string.Empty;
        
        public string Status { get; set; } = string.Empty; // IN, OUT, AUDIT, etc.
    }
}