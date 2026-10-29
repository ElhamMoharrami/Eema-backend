package com.eema.backend.service;

import com.eema.backend.config.AppProperties;
import com.eema.backend.entity.Booking;
import com.eema.backend.repository.BookingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.*;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class OperationsReportingTest {
    static final String FIRST="https://api.mollie.com/v2/balances/primary/transactions?limit=250";
    AppProperties properties(boolean enabled,String token){return new AppProperties(null,null,null,new AppProperties.MollieProperties(null,null,null,token,enabled,1000,1000),null,null,null,null,null);}
    String page(String fee,String next){return "{\"_embedded\":{\"balance_transactions\":[{\"type\":\"payment\",\"context\":{\"paymentId\":\"tr_qa\"},\"deductionDetails\":"+fee+"}]},\"_links\":{\"next\":{\"href\":"+(next==null?"null":"\""+next+"\"")+"}}}";}
    @Test void absentTokenDoesNotCallTransport(){var rest=mock(RestTemplate.class);assertTrue(new HttpMollieFeeReportingClient(rest,properties(true,"")).findAuthoritativePaymentFee("tr_qa").isEmpty());verifyNoInteractions(rest);}
    @Test void noFeeIsNotInvented(){var rest=new RestTemplate();var server=MockRestServiceServer.bindTo(rest).build();server.expect(requestTo(FIRST)).andRespond(withSuccess(page("{}",null),MediaType.APPLICATION_JSON));assertTrue(new HttpMollieFeeReportingClient(rest,properties(true,"qa-reporting")).findAuthoritativePaymentFee("tr_qa").isEmpty());server.verify();}
    @Test void sameOriginPaginationFindsExactEuroFee(){var rest=new RestTemplate();var server=MockRestServiceServer.bindTo(rest).build();String next="https://api.mollie.com/v2/balances/primary/transactions?from=qa";server.expect(requestTo(FIRST)).andExpect(header("Authorization","Bearer qa-reporting")).andRespond(withSuccess("{\"_embedded\":{\"balance_transactions\":[]},\"_links\":{\"next\":{\"href\":\""+next+"\"}}}",MediaType.APPLICATION_JSON));server.expect(requestTo(next)).andRespond(withSuccess(page("{\"fees\":{\"currency\":\"EUR\",\"value\":\"0.49\"}}",null),MediaType.APPLICATION_JSON));assertEquals(new BigDecimal("0.49"),new HttpMollieFeeReportingClient(rest,properties(true,"qa-reporting")).findAuthoritativePaymentFee("tr_qa").orElseThrow());server.verify();}
    @ParameterizedTest @ValueSource(strings={"USD","invalid-number","empty","malformed","provider"})
    void invalidReportingCannotInventFee(String mode){var rest=new RestTemplate();var server=MockRestServiceServer.bindTo(rest).build();var expected=server.expect(requestTo(FIRST));if(mode.equals("provider"))expected.andRespond(withServerError());else if(mode.equals("empty"))expected.andRespond(withSuccess("",MediaType.APPLICATION_JSON));else if(mode.equals("malformed"))expected.andRespond(withSuccess("{",MediaType.APPLICATION_JSON));else expected.andRespond(withSuccess(page("{\"fees\":{\"currency\":\""+(mode.equals("USD")?"USD":"EUR")+"\",\"value\":\""+(mode.equals("USD")?"0.49":"bad")+"\"}}",null),MediaType.APPLICATION_JSON));assertThrows(RuntimeException.class,()->new HttpMollieFeeReportingClient(rest,properties(true,"qa-reporting")).findAuthoritativePaymentFee("tr_qa"));server.verify();}
    @ParameterizedTest @ValueSource(strings={"https://outside.example.test/fees","https://api.mollie.com.attacker.example.test/fees","https://api.mollie.com/v2/balances/primary/transactions?limit=250"})
    void unsafeOrLoopingPaginationStopsBeforeAnotherRequest(String next){var rest=new RestTemplate();var server=MockRestServiceServer.bindTo(rest).build();server.expect(requestTo(FIRST)).andRespond(withSuccess("{\"_embedded\":{\"balance_transactions\":[]},\"_links\":{\"next\":{\"href\":\""+next+"\"}}}",MediaType.APPLICATION_JSON));assertThrows(IllegalStateException.class,()->new HttpMollieFeeReportingClient(rest,properties(true,"qa-reporting")).findAuthoritativePaymentFee("tr_qa"));server.verify();}
    @Test void disabledJobNeverQueriesOrCallsProvider(){var repo=mock(BookingRepository.class);var client=mock(MollieFeeReportingClient.class);var service=mock(BookingService.class);new MollieFeeReconciliationJob(properties(false,"qa"),repo,client,service).reconcile();verifyNoInteractions(repo,client,service);}
    @Test void oneFailureAndOneUnavailableFeeDoNotPreventNextBooking(){var repo=mock(BookingRepository.class);var client=mock(MollieFeeReportingClient.class);var service=mock(BookingService.class);var failed=Booking.builder().id(UUID.randomUUID()).molliePaymentId("tr_failed").build();var absent=Booking.builder().id(UUID.randomUUID()).molliePaymentId("tr_absent").build();var valid=Booking.builder().id(UUID.randomUUID()).molliePaymentId("tr_valid").build();when(repo.findPendingFeeReconciliation(any())).thenReturn(List.of(failed,absent,valid));when(client.findAuthoritativePaymentFee("tr_failed")).thenThrow(new IllegalStateException("QA unavailable"));when(client.findAuthoritativePaymentFee("tr_absent")).thenReturn(Optional.empty());when(client.findAuthoritativePaymentFee("tr_valid")).thenReturn(Optional.of(new BigDecimal("0.49")));new MollieFeeReconciliationJob(properties(true,"qa"),repo,client,service).reconcile();verify(repo).findPendingFeeReconciliation(org.springframework.data.domain.PageRequest.of(0,100));verify(service).reconcileMollieFee(valid.getId(),new BigDecimal("0.49"));verifyNoMoreInteractions(service);verify(repo,never()).save(any());}
}
