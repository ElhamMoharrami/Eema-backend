package com.eema.backend.controller;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import static org.assertj.core.api.Assertions.assertThat;

class TrustWorkflowSecuritySurfaceTest {
    @Test void adminQueuesAndActionsAreAdminOnly(){
        assertThat(AdminOwnerVerificationController.class.getAnnotation(PreAuthorize.class).value()).isEqualTo("hasRole('ADMIN')");
        assertThat(AdminVenueModerationController.class.getAnnotation(PreAuthorize.class).value()).isEqualTo("hasRole('ADMIN')");
    }
    @Test void ownerVerificationAndSubmissionAreOwnerOnly() throws Exception {
        assertThat(OwnerVerificationController.class.getAnnotation(PreAuthorize.class).value()).contains("OWNER");
        assertThat(VenueController.class.getMethod("submitForReview",java.util.UUID.class).getAnnotation(PreAuthorize.class).value())
                .contains("OWNER").contains("verifiedUserPolicy");
    }
}
