package com.eema.backend.service;

import com.eema.backend.config.AppProperties;
import com.eema.backend.controller.WebhookController;
import com.eema.backend.dto.response.MolliePaymentResponse;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.test.web.client.MockRestServiceServer;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class PaymentTransportContractTest {
 AppProperties props(String webhook){return new AppProperties(null,null,null,new AppProperties.MollieProperties("test_qa_only","http://localhost:3000/booked",webhook,null,false,3600000,60000),null,null,null,null,null);}
 @ParameterizedTest @ValueSource(strings={"", "https://webhook.example.test/api/webhooks/mollie/payment"}) void actualTransportSendsServerAssociationAndIdempotency(String webhook)throws Exception{
  var rest=new RestTemplate();var server=MockRestServiceServer.bindTo(rest).build();UUID booking=UUID.randomUUID(),key=UUID.randomUUID();server.expect(requestTo("https://api.mollie.com/v2/payments")).andExpect(method(HttpMethod.POST)).andExpect(header("Idempotency-Key",key.toString())).andExpect(request->{var json=new com.fasterxml.jackson.databind.ObjectMapper().readTree(((org.springframework.mock.http.client.MockClientHttpRequest)request).getBodyAsString());assertEquals("20.32",json.path("amount").path("value").asText());assertEquals("EUR",json.path("amount").path("currency").asText());assertEquals(booking.toString(),json.path("metadata").path("bookingId").asText());assertEquals("http://localhost:3000/booked",json.path("redirectUrl").asText());assertEquals(!webhook.isBlank(),json.has("webhookUrl"));}).andRespond(withSuccess("{\"id\":\"tr_qa\",\"status\":\"open\"}",MediaType.APPLICATION_JSON));new HttpMollieProviderClient(rest,props(webhook)).createPayment(booking,new BigDecimal("20.32"),key);server.verify();
 }
 @ParameterizedTest @ValueSource(strings={"", "{", "[]"}) void malformedTransportJsonCannotProducePaidObject(String body){var rest=new RestTemplate();var server=MockRestServiceServer.bindTo(rest).build();server.expect(requestTo("https://api.mollie.com/v2/payments/tr_qa")).andRespond(withSuccess(body,MediaType.APPLICATION_JSON));assertThrows(RuntimeException.class,()->new HttpMollieProviderClient(rest,props("")).getPayment("tr_qa"));server.verify();}
 @Test void providerFailureLogContainsNoRawExceptionOrBody(){var service=mock(WebhookService.class);doThrow(new org.springframework.web.client.RestClientException("QA_PRIVATE_PROVIDER_BODY")).when(service).handlePaymentWebhook("tr_qa");var logger=(ch.qos.logback.classic.Logger)org.slf4j.LoggerFactory.getLogger(WebhookController.class);var capture=new ch.qos.logback.core.read.ListAppender<ch.qos.logback.classic.spi.ILoggingEvent>();capture.start();logger.addAppender(capture);try{assertEquals(500,new WebhookController(service).handlePaymentWebhook("tr_qa").getStatusCode().value());for(var event:capture.list){assertFalse(event.getFormattedMessage().contains("QA_PRIVATE_PROVIDER_BODY"));assertNull(event.getThrowableProxy());}}finally{logger.detachAppender(capture);}}
 @Test void missingPoliciesStopBeforeClaimOrProvider(){var coordinator=mock(PaymentCreationCoordinator.class);var provider=mock(MollieProviderClient.class);var users=mock(CurrentUserService.class);var policies=mock(PolicyAcceptanceService.class);var user=com.eema.backend.entity.User.builder().id(UUID.randomUUID()).build();when(users.getCurrentUser()).thenReturn(user);doThrow(new com.eema.backend.exception.ForbiddenException("Current required policies must be accepted")).when(policies).requireCurrentPolicies(user);assertThrows(com.eema.backend.exception.ForbiddenException.class,()->new PaymentService(coordinator,provider,users,policies).createPayment(UUID.randomUUID(),user.getId()));verifyNoInteractions(coordinator,provider);}
}
