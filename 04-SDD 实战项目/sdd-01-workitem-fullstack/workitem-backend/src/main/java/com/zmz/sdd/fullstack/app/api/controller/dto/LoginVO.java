package com.zmz.sdd.fullstack.app.api.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/** [SDD-SPEC: 02-功能规范.md §3.6] */
@Data
@AllArgsConstructor
public class LoginVO {
    private String token;
    private long expiresIn;
    private String username;
}
