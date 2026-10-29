package com.eema.backend.dto.response;
import java.util.List;
public record PolicyStatusDto(boolean allRequiredAccepted,List<PolicyVersionDto> missing){}
