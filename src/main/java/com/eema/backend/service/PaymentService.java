package com.eema.backend.service;

import com.eema.backend.config.AppProperties;
import com.eema.backend.dto.response.CheckoutResponse;
import com.eema.backend.dto.response.MolliePaymentResponse;
import com.eema.backend.entity.Booking;
import com.eema.backend.entity.enums.BookingStatus;
import com.eema.backend.exception.BadRequestException;
import com.eema.backend.exception.NotFoundException;
import com.eema.backend.repository.BookingRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final String MOLLIE_API_BASE =
            "https://api.mollie.com/v2";

    private final RestTemplate restTemplate;
    private final AppProperties appProperties;
    private final BookingRepository bookingRepository;

    public CheckoutResponse createPayment(
            UUID bookingId
    ) {

        Booking booking =
                bookingRepository
                        .findById(bookingId)
                        .orElseThrow(() ->
                                new NotFoundException(
                                        "Booking not found: "
                                                + bookingId
                                )
                        );

        if (booking.getStatus()
                != BookingStatus.PENDING) {

            throw new BadRequestException(
                    "Only pending bookings can be paid"
            );
        }

        if (booking.getMolliePaymentId() != null
                && !booking
                .getMolliePaymentId()
                .isBlank()) {

            throw new BadRequestException(
                    "Payment has already been created for this booking"
            );
        }

        BigDecimal amount =
                booking.getVenuePriceEur()
                        .setScale(
                                2,
                                RoundingMode.HALF_UP
                        );

        log.info(
                "Creating Mollie payment: bookingId={}, amount={}",
                bookingId,
                amount
        );

        try {

            MolliePaymentResponse payment =
                    callMollieCreatePayment(
                            bookingId,
                            amount
                    );

            if (payment.id() == null
                    || payment.id().isBlank()) {

                throw new IllegalStateException(
                        "Mollie returned no payment ID"
                );
            }

            if (payment.links() == null
                    || payment.links().checkout() == null
                    || payment.links().checkout().href() == null) {

                throw new IllegalStateException(
                        "Mollie returned no checkout URL"
                );
            }

            booking.setMolliePaymentId(
                    payment.id()
            );

            bookingRepository.save(
                    booking
            );

            return new CheckoutResponse(
                    bookingId,
                    payment.links()
                            .checkout()
                            .href(),
                    payment.id(),
                    amount.doubleValue(),
                    300L
            );

        } catch (RestClientException e) {

            log.error(
                    "Failed to create Mollie payment for booking {}",
                    bookingId,
                    e
            );

            throw new BadRequestException(
                    "Could not initiate payment"
            );
        }
    }

    private MolliePaymentResponse callMollieCreatePayment(
            UUID bookingId,
            BigDecimal amount
    ) {

        String apiKey =
                appProperties
                        .mollie()
                        .apiKey();

        String redirectUrl =
                appProperties
                        .mollie()
                        .redirectUrl();

        String webhookUrl =
                appProperties
                        .mollie()
                        .webhookUrl();

        if (apiKey == null
                || apiKey.isBlank()) {

            throw new IllegalStateException(
                    "Mollie API key is not configured"
            );
        }

        if (webhookUrl == null
                || webhookUrl.isBlank()) {

            throw new IllegalStateException(
                    "Mollie webhook URL is not configured"
            );
        }

        String url =
                MOLLIE_API_BASE
                        + "/payments";

        Map<String, Object> body =
                new HashMap<>();

        body.put(
                "amount",
                Map.of(
                        "value",
                        amount.toPlainString(),
                        "currency",
                        "EUR"
                )
        );

        body.put(
                "description",
                "Eema booking #" + bookingId
        );

        body.put(
                "redirectUrl",
                redirectUrl
        );

        body.put(
                "webhookUrl",
                webhookUrl
        );

        body.put(
                "metadata",
                Map.of(
                        "bookingId",
                        bookingId.toString()
                )
        );

        HttpHeaders headers =
                new HttpHeaders();

        headers.setBearerAuth(apiKey);

        headers.setContentType(
                MediaType.APPLICATION_JSON
        );

        headers.setAccept(
                List.of(
                        MediaType.APPLICATION_JSON
                )
        );

        ResponseEntity<MolliePaymentResponse> response =
                restTemplate.exchange(
                        url,
                        HttpMethod.POST,
                        new HttpEntity<>(
                                body,
                                headers
                        ),
                        MolliePaymentResponse.class
                );

        MolliePaymentResponse payment =
                response.getBody();

        if (payment == null) {

            throw new IllegalStateException(
                    "Mollie returned an empty response"
            );
        }

        return payment;
    }
}