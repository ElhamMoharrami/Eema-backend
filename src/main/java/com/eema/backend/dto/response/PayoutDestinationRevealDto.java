package com.eema.backend.dto.response;
import java.util.UUID;
public record PayoutDestinationRevealDto(UUID id,String accountHolderName,String iban){}
