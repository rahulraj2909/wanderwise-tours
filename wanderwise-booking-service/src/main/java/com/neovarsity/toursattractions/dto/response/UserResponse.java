package com.neovarsity.toursattractions.dto.response;

import com.neovarsity.toursattractions.entity.enums.UserRole;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserResponse {

    private Long id;
    private String name;
    private String email;
    private String phone;
    private UserRole role;
}
