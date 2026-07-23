package com.eema.backend.service;

import com.eema.backend.config.AppProperties;
import com.resend.core.exception.ResendException;
import com.resend.Resend;
import com.resend.services.emails.Emails;
import com.resend.services.emails.model.SendEmailRequest;
import com.resend.services.emails.model.SendEmailResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EmailServiceTest {

    @Mock
    private AppProperties appProperties;

    @Mock
    private Resend mockResendClient;

    @Mock
    private Emails mockEmails;

    private EmailService emailService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Mock AppProperties.ResendProperties
        AppProperties.ResendProperties resendProps = new AppProperties.ResendProperties();
        resendProps.setApiKey("test_resend_key");
        resendProps.setFromEmail("test@eema.com");
        when(appProperties.getResend()).thenReturn(resendProps);

        // Mock the Resend client's emails() method to return a mock Emails object
        when(mockResendClient.emails()).thenReturn(mockEmails);

        // Create EmailService with the mocked Resend client (using the test constructor)
        emailService = new EmailService(appProperties, mockResendClient);
    }

    @Test
    void sendBookingConfirmationToScout_success() throws ResendException {
        // Arrange
        SendEmailResponse mockResponse = new SendEmailResponse();
        mockResponse.setId("test_email_id_123");
        when(mockEmails.send(any(SendEmailRequest.class))).thenReturn(mockResponse);

        // Act
        emailService.sendBookingConfirmationToScout(
                "scout@example.com",
                "Venue Name, Amsterdam",
                "Owner Name (+123)",
                "https://calendly.com/eema/123"
        );

        // Assert
        verify(mockEmails).send(any(SendEmailRequest.class));

        // Verify email contents
        ArgumentCaptor<SendEmailRequest> requestCaptor = ArgumentCaptor.forClass(SendEmailRequest.class);
        verify(mockEmails).send(requestCaptor.capture());

        SendEmailRequest capturedRequest = requestCaptor.getValue();
        assertEquals("test@eema.com", capturedRequest.getFrom());
        assertEquals(Collections.singletonList("scout@example.com"), capturedRequest.getTo());
        assertEquals("Your Eema booking is confirmed!", capturedRequest.getSubject());
        assertTrue(capturedRequest.getHtml().contains("<h2>Your Eema booking is confirmed!</h2>"));
        assertTrue(capturedRequest.getHtml().contains("Venue Name, Amsterdam"));
        assertTrue(capturedRequest.getHtml().contains("Owner Name (+123)"));
        assertTrue(capturedRequest.getHtml().contains("<a href=\"https://calendly.com/eema/123\">https://calendly.com/eema/123</a>"));
    }

    @Test
    void sendBookingNotificationToOwner_success() throws ResendException {
        // Arrange
        SendEmailResponse mockResponse = new SendEmailResponse();
        mockResponse.setId("test_email_id_456");
        when(mockEmails.send(any(SendEmailRequest.class))).thenReturn(mockResponse);

        // Act
        emailService.sendBookingNotificationToOwner(
                "owner@example.com",
                "Scout Name (+456)",
                "2026-07-20T15:00:00"
        );

        // Assert
        verify(mockEmails).send(any(SendEmailRequest.class));

        // Verify email contents
        ArgumentCaptor<SendEmailRequest> requestCaptor = ArgumentCaptor.forClass(SendEmailRequest.class);
        verify(mockEmails).send(requestCaptor.capture());

        SendEmailRequest capturedRequest = requestCaptor.getValue();
        assertEquals("test@eema.com", capturedRequest.getFrom());
        assertEquals(Collections.singletonList("owner@example.com"), capturedRequest.getTo());
        assertEquals("New booking request for your venue", capturedRequest.getSubject());
        assertTrue(capturedRequest.getHtml().contains("<h2>New booking request for your venue</h2>"));
        assertTrue(capturedRequest.getHtml().contains("<p><strong>Scout Contact:</strong> Scout Name (+456)</p>"));
        assertTrue(capturedRequest.getHtml().contains("<p>Please confirm this booking within 24 hours (by 2026-07-20T15:00:00).</p>"));
    }

    @Test
    void sendBookingConfirmationToScout_throwsResendException_whenApiFails() throws ResendException {
        // Arrange
        when(mockEmails.send(any(SendEmailRequest.class))).thenThrow(new ResendException("API Error"));

        // Act & Assert
        ResendException thrown = assertThrows(
                ResendException.class,
                () -> emailService.sendBookingConfirmationToScout(
                        "scout@example.com",
                        "Venue Name, Amsterdam",
                        "Owner Name (+123)",
                        "https://calendly.com/eema/123"
                ),
                "Expected sendBookingConfirmationToScout to throw ResendException, but it didn't"
        );

        assertEquals("API Error", thrown.getMessage());
    }

    @Test
    void sendBookingNotificationToOwner_throwsResendException_whenApiFails() throws ResendException {
        // Arrange
        when(mockEmails.send(any(SendEmailRequest.class))).thenThrow(new ResendException("API Error"));

        // Act & Assert
        ResendException thrown = assertThrows(
                ResendException.class,
                () -> emailService.sendBookingNotificationToOwner(
                        "owner@example.com",
                        "Scout Name (+456)",
                        "2026-07-20T15:00:00"
                ),
                "Expected sendBookingNotificationToOwner to throw ResendException, but it didn't"
        );

        assertEquals("API Error", thrown.getMessage());
    }

    @Test
    void sendPaymentConfirmationEmail_doesNothing() {
        // This method should do nothing and not throw any exceptions
        assertDoesNotThrow(() -> emailService.sendPaymentConfirmationEmail(java.util.UUID.randomUUID()));
        // No interactions with mocks expected
        verifyNoInteractions(mockEmails);
    }
}