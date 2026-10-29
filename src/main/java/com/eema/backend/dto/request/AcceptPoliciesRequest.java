package com.eema.backend.dto.request;
import jakarta.validation.constraints.*;
import java.util.*;
public record AcceptPoliciesRequest(@NotEmpty @Size(max=5) Set<@NotNull UUID> policyVersionIds){}
