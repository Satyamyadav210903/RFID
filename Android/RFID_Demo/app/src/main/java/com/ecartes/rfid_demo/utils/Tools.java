package com.ecartes.rfid_demo.utils;

public class Tools {
    
    /**
     * Convert hex string to byte array
     */
    public static byte[] HexString2Bytes(String hex) {
        if (hex == null || hex.length() % 2 != 0) {
            return null;
        }
        
        hex = hex.toUpperCase();
        int len = hex.length() / 2;
        byte[] result = new byte[len];
        
        for (int i = 0; i < len; i++) {
            String tmp = hex.substring(i * 2, i * 2 + 2);
            result[i] = (byte) Integer.parseInt(tmp, 16);
        }
        
        return result;
    }
    
    /**
     * Convert byte array to hex string
     */
    public static String Bytes2HexString(byte[] b, int len) {
        StringBuilder sb = new StringBuilder();
        
        for (int i = 0; i < len; i++) {
            String hex = Integer.toHexString(b[i] & 0xFF);
            if (hex.length() == 1) {
                hex = '0' + hex;
            }
            sb.append(hex.toUpperCase());
        }
        
        return sb.toString();
    }
}