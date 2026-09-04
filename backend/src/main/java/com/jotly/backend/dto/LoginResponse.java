package com.jotly.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponse {

    private String message;
    private String username;
    private String token;
    public LoginResponse(String message,String username){
        this.message=message;
        this.username=username;
    }
}