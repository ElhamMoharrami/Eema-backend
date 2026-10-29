package com.eema.backend.service;

import com.eema.backend.config.AppProperties;
import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.SendEmailRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** Thin provider adapter. Business email content belongs outside this class. */
@Service
@Slf4j
public class EmailService {
    private final boolean enabled;
    private final Resend resendClient;
    private final String fromEmail;

    public EmailService(AppProperties properties) {
        AppProperties.ResendProperties resend = properties.resend();
        this.enabled = resend != null && resend.enabled();
        this.fromEmail = resend == null ? null : resend.fromEmail();
        if (enabled && (!StringUtils.hasText(resend.apiKey()) || !StringUtils.hasText(fromEmail))) {
            throw new IllegalStateException("Email notifications are enabled but RESEND_API_KEY or RESEND_FROM_EMAIL is missing");
        }
        this.resendClient = enabled ? new Resend(resend.apiKey()) : null;
        if (!enabled) log.info("Transactional email notifications are disabled");
    }

    EmailService(boolean enabled, String fromEmail, Resend resendClient) {
        this.enabled = enabled;
        this.fromEmail = fromEmail;
        this.resendClient = resendClient;
    }

    public void send(String recipient, String subject, String text, String html) {
        if (!enabled) {
            log.info("Transactional email skipped because notifications are disabled");
            return;
        }
        SendEmailRequest request = SendEmailRequest.builder()
                .from(fromEmail).to(recipient).subject(subject).text(text).html(html).build();
        try {
            resendClient.emails().send(request);
        } catch (ResendException | RuntimeException exception) {
            throw new EmailDeliveryException("Resend email delivery failed", exception);
        }
    }

    public static class EmailDeliveryException extends RuntimeException {
        EmailDeliveryException(String message, Throwable cause) { super(message, cause); }
    }
}
