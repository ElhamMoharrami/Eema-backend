package com.eema.backend.controller;

import com.eema.backend.dto.request.CreateAvailabilityInquiryRequest;
import com.eema.backend.dto.request.CreateBookingRequest;
import com.eema.backend.dto.request.CreateVenueRequest;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import static org.assertj.core.api.Assertions.assertThat;

class VerifiedEmailEnforcementSurfaceTest {
    @Test
    void launchBlockerMarketplaceMutationsUseCentralVerifiedUserPolicy() throws Exception {
        assertVerified(BookingController.class, "createBooking", CreateBookingRequest.class);
        assertVerified(AvailabilityInquiryController.class, "create", CreateAvailabilityInquiryRequest.class);
        assertVerified(VenueController.class, "createVenue", CreateVenueRequest.class);
        assertVerified(CheckoutController.class, "createCheckout", java.util.UUID.class);
    }

    private static void assertVerified(Class<?> controller, String method, Class<?>... parameters) throws Exception {
        PreAuthorize rule = controller.getMethod(method, parameters).getAnnotation(PreAuthorize.class);
        assertThat(rule).isNotNull();
        assertThat(rule.value()).contains("@verifiedUserPolicy.isVerified()");
    }
}
