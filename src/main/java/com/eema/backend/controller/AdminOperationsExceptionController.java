package com.eema.backend.controller;

import com.eema.backend.dto.response.OperationalExceptionDto;
import com.eema.backend.service.AdminOperationsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/operations")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminOperationsExceptionController {
    private final AdminOperationsService operations;
    @GetMapping("/exceptions")
    public Page<OperationalExceptionDto> exceptions(@PageableDefault(size = 20) Pageable pageable) {
        return operations.exceptions(pageable);
    }
}
