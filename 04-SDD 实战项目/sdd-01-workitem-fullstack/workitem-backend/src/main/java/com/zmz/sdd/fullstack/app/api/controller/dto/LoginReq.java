package com.zmz.sdd.fullstack.app.api.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** [SDD-SPEC: 02-功能规范.md §3.6] */
@Data
public class LoginReq {

    @NotBlank(message = "is required")
    @Size(min = 3, max = 50, message = "length must be 3~50")
    private String username;

    @NotBlank(message = "is required")
    private String password;
}
