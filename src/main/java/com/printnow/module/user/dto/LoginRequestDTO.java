package com.printnow.module.user.dto;

import lombok.Data;

@Data
public class LoginRequestDTO {
    private String email;
    private String motDePasse;
}
