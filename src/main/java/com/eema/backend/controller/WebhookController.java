package com.eema.backend.controller;

import com.eema.backend.service.WebhookService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/webhooks/mollie")
@RequiredArgsConstructor
public class WebhookController {

    private final WebhookService webhookService;

    @PostMapping(value = "/payment", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<Void> handlePaymentWebhook(@RequestParam("id") String paymentId) {

        try {

            webhookService.handlePaymentWebhook(paymentId);

            return ResponseEntity.ok().build();

        } catch (IllegalArgumentException e) {

            log.warn("Invalid Mollie webhook request");

            return ResponseEntity.badRequest().build();

        } catch (Exception e) {

            log.error("Failed to process Mollie webhook: paymentId={}, errorType={}", paymentId, e.getClass().getSimpleName());

            return ResponseEntity.internalServerError().build();
        }
    }
}