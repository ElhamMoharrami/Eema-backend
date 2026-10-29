package com.eema.backend.dto.request;
import jakarta.validation.constraints.*;
public record CreatePayoutDestinationRequest(@NotBlank @Size(max=160) String accountHolderName,@NotBlank @Size(max=80) String iban){}
