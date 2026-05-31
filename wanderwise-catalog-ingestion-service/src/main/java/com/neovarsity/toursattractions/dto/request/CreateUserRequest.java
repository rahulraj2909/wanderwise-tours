package com.neovarsity.toursattractions.dto.request;

import com.neovarsity.toursattractions.entity.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateUserRequest {

    @NotBlank
    private String name;

    @NotBlank
    @Email
    private String email;

    private String phone;

    @NotNull
    private UserRole role;
}
