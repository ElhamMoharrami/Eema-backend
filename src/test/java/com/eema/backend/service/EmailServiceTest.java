package com.eema.backend.service;

import com.eema.backend.config.AppProperties;
import com.resend.Resend;
import com.resend.services.emails.Emails;
import com.resend.services.emails.model.SendEmailRequest;
import com.resend.services.emails.model.SendEmailResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private AppProperties appProperties;

    @Mock
    private Resend resendClient;

    @Mock
    private Emails emails;

    @InjectMocks
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        // Mock the ResendProperties to return a test API key and from email
        when(appProperties.getResend()).thenReturn(new AppProperties.ResendProperties() {{
            setApiKey("test_api_key");
            setFromEmail("Eema <test@eema.co>");
        }});
        when(resendClient.emails()).thenReturn(emails);
    }

    @Test
    void testSendBookingConfirmationToScout_Success() {
        // Arrange
        SendEmailRequest request = SendEmailRequest.builder()
                .from("Eema <test@eema.co>") // Should match the mocked from email
                .to(Collections.singletonList("scout@example.com"))
                .subject("Your Eema booking is confirmed!")
                .html("<p>Test</p>")
                .build();

        SendEmailResponse response = new SendEmailResponse("email_id_123");

        when(emails.send(request)).thenReturn(response);

        // Act
        emailService.sendBookingConfirmationToScout(
                "scout@example.com",
                "Venue Name, 123 Street",
                "Owner: John Doe, 555-1234",
                "https://calendly.com/eema/123"
        );

        // Assert
        verify(emails).send(request);
    }

    @Test
    void testSendBookingConfirmationToScout_Failure() {
        // Arrange
        SendEmailRequest request = SendEmailRequest.builder()
                .from("Eema <test@eema.co>") // Should match the mocked from email
                .to(Collections.singletonList("scout@example.com"))
                .subject("Your Eema booking is confirmed!")
                .html("<p>Test</p>")
                .build();

        when(emails.send(request)).thenThrow(new RuntimeException("Resend error"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            emailService.sendBookingConfirmationToScout(
                    "scout@example.com",
                    "Venue Name, 123 Street",
                    "Owner: John Doe, 555-1234",
                    "https://calendly.com/eema/123"
            );
        });
    }

    @Test
    void testSendBookingNotificationToOwner_Success() {
        // Arrange
        SendEmailRequest request = SendEmailRequest.builder()
                .from("Eema <test@eema.co>") // Should match the mocked from email
                .to(Collections.singletonList("owner@example.com"))
                .subject("New booking request for your venue")
                .html("<p>Test</p>")
                .build();

        SendEmailResponse response = new SendEmailResponse("email_id_456");

        when(emails.send(request)).thenReturn(response);

        // Act
        emailService.sendBookingNotificationToOwner(
                "owner@example.com",
                "Scout: Jane Doe, jane@example.com, 555-5678",
                "2026-07-19T10:00:00:00"
        );

        // Assert
        verify(emails).send": "30"
        );

        // Assert
        verify(emails).send(request);
    }

    @Test
    void testSendBookingNotificationToOwner_Failure() {
        // Arrange
        SendEmailRequest request = SendEmailRequest.builder()
                .from("Eema <test@eema.co>") // Should match the mocked from email
                .to(Collections.singletonList("owner@example.com"))
                .subject("New booking request for your venue")
                .html("<p>Test</p>")
                .build();

        when(emails.send(request)).thenThrow(new RuntimeException("Resend error"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            emailService.sendBookingNotificationToOwner(
                    "owner@example.com",
                    "Scout: Jane Doe, jane@example.com, 555-5678",
                    "2026-07-19T10:00:00"
            );
        });
    }
}