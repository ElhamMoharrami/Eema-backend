package com.eema.backend.controller;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.lang.reflect.Method;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;

class AdminOperationsSurfaceTest {
    private static final List<Class<?>> ADMIN_CONTROLLERS = List.of(
            AdminOwnerPayoutController.class, AdminOwnerFinancialLiabilityController.class,
            AdminBookingOperationsController.class, AdminOperationsExceptionController.class);

    @Test
    void everyAdminControllerHasDefenseInDepthRoleGuard() {
        ADMIN_CONTROLLERS.forEach(type -> assertThat(type.getAnnotation(PreAuthorize.class))
                .as(type.getSimpleName()).isNotNull()
                .extracting(PreAuthorize::value).isEqualTo("hasRole('ADMIN')"));
    }

    @Test
    void surfaceContainsNoGenericPutPatchOrBookingStatusMutation() {
        for (Class<?> type : ADMIN_CONTROLLERS) {
            assertThat(type.getDeclaredMethods()).noneMatch(m -> m.isAnnotationPresent(PutMapping.class)
                    || m.isAnnotationPresent(PatchMapping.class));
        }
        Set<String> postPaths = new HashSet<>();
        for (Class<?> type : ADMIN_CONTROLLERS) for (Method method : type.getDeclaredMethods()) {
            PostMapping mapping = method.getAnnotation(PostMapping.class);
            if (mapping != null) postPaths.addAll(Arrays.asList(mapping.value()));
        }
        assertThat(postPaths).containsExactlyInAnyOrder("/{id}/mark-paid", "/{id}/settle");
    }
}
