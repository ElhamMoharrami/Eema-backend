package com.eema.backend.service;

import com.eema.backend.config.AppProperties;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.http.*;
import org.springframework.web.client.*;
import org.springframework.test.web.client.MockRestServiceServer;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

/** Exercises our HTTP encoding/decoding against a scripted server, never live Mollie. */
class RefundTransportContractTest {
    final RestTemplate rest = new RestTemplate();
    final MockRestServiceServer server = MockRestServiceServer.bindTo(rest).build();
    final HttpMollieProviderClient client = new HttpMollieProviderClient(rest,new AppProperties(null,null,null,
            new AppProperties.MollieProperties("test_qa_only","https://example.test/return",null,null,false,3600000,60000),null,null,null,null,null));
    final String url="https://api.mollie.com/v2/payments/tr_original/refunds";
    final String body="{\"id\":\"re_original\",\"status\":\"queued\",\"paymentId\":\"tr_original\",\"amount\":{\"value\":\"500.00\",\"currency\":\"EUR\"}}";
    @Test void createAndLookupUsePaymentScopeExactAmountAndDurableKey() {
        UUID booking=UUID.randomUUID(),key=UUID.randomUUID();
        server.expect(requestTo(url)).andExpect(method(HttpMethod.POST)).andExpect(header("Idempotency-Key",key.toString()))
                .andExpect(content().json("{\"amount\":{\"value\":\"500.00\",\"currency\":\"EUR\"},\"metadata\":{\"bookingId\":\""+booking+"\"}}"))
                .andRespond(withSuccess(body,MediaType.APPLICATION_JSON));
        server.expect(requestTo(url+"/re_original")).andExpect(method(HttpMethod.GET)).andExpect(headerDoesNotExist("Idempotency-Key"))
                .andRespond(withSuccess(body.replace("queued","refunded"),MediaType.APPLICATION_JSON));
        assertEquals("queued",client.createRefund("tr_original",booking,new BigDecimal("500.00"),key).status());
        assertEquals("refunded",client.getRefund("tr_original","re_original").status()); server.verify();
    }
    @ParameterizedTest @ValueSource(ints={422,429,500,502}) void providerErrorsRemainErrors(int status) {
        server.expect(requestTo(url)).andRespond(withStatus(HttpStatus.valueOf(status)));
        assertThrows(HttpStatusCodeException.class,()->client.createRefund("tr_original",UUID.randomUUID(),new BigDecimal("500.00"),UUID.randomUUID()));server.verify();
    }
    @Test void responseLossIsTransportAmbiguity() {
        server.expect(requestTo(url)).andRespond(request->{throw new java.io.IOException("Response lost after acceptance");});
        assertThrows(ResourceAccessException.class,()->client.createRefund("tr_original",UUID.randomUUID(),new BigDecimal("500.00"),UUID.randomUUID()));server.verify();
    }
    @Test void paymentCancellationUsesDeleteAndDecodesProviderState() {
        server.expect(requestTo("https://api.mollie.com/v2/payments/tr_original")).andExpect(method(HttpMethod.DELETE))
                .andRespond(withSuccess("{\"id\":\"tr_original\",\"status\":\"canceled\"}",MediaType.APPLICATION_JSON));
        assertEquals("canceled",client.cancelPayment("tr_original").status());server.verify();
    }
}
