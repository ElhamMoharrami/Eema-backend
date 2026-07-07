package com.eema.backend.dto.request;

import com.eema.backend.entity.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegisterRequest {
    @NotBlank @Email String email;
    @NotBlank @Size(min = 8, max = 72) String password;
    @NotBlank String fullName;
    UserRole role;
}