package com.ecartes.rfid_demo.model;

public class LoginResponse {
    private String token;
    private String expiration;
    
    public LoginResponse() {}
    
    public LoginResponse(String token, String expiration) {
        this.token = token;
        this.expiration = expiration;
    }
    
    public String getToken() {
        return token;
    }
    
    public void setToken(String token) {
        this.token = token;
    }
    
    public String getExpiration() {
        return expiration;
    }
    
    public void setExpiration(String expiration) {
        this.expiration = expiration;
    }
}