package com.eema.backend.service;

import com.eema.backend.entity.*;
import com.eema.backend.entity.enums.*;
import com.eema.backend.config.AppProperties;
import com.eema.backend.dto.response.MolliePaymentResponse;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.ArgumentCaptor;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import java.util.*;
import java.time.*;
import java.math.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PriceDownstreamContractTest {
 Booking booking(){return Booking.builder().id(UUID.randomUUID()).status(BookingStatus.CONFIRMED).shootDate(LocalDate.of(2026,11,20)).startTime(LocalTime.of(9,0)).endTime(LocalTime.of(10,1)).venuePriceEur(new BigDecimal("20.32")).hourlyRateEurSnapshot(new BigDecimal("19.99")).minimumBookingMinutesSnapshot(1).fullDayRateEurSnapshot(new BigDecimal("500.00")).durationMinutes(61).billableMinutes(61).totalPaidEur(new BigDecimal("20.32")).commissionPercentage(new BigDecimal("20.00")).paidAt(OffsetDateTime.parse("2026-10-01T00:00:00Z")).scoutContact(BookingContactSnapshot.builder().name("QA Price Scout").email("price@example.test").build()).build();}
 @ParameterizedTest @CsvSource({"8,20.32,0.00","3,10.16,2.03","1,0.00,4.06"})
 void cancellationUsesPaidSnapshotAndStoredCommission(int days,String refund,String commission){var b=booking();var p=new BookingCancellationPolicy(new BookingFinancials());p.snapshotAtConfirmation(b,ZoneId.of("Europe/Amsterdam"),b.getPaidAt());var result=p.evaluate(b,ActorType.SCOUT,b.getShootDate().minusDays(days).atTime(0,0).atOffset(ZoneOffset.UTC));assertEquals(new BigDecimal(refund),result.refundAmountEur());assertEquals(new BigDecimal(commission),result.eemaCommissionAfterCancellationEur());assertEquals(new BigDecimal("20.32"),result.refundAmountEur().add(result.retainedAmountEur()));}
 @Test void receiptUsesSavedMonetaryFieldsEvenWhenVenuePricingChanges()throws Exception{var v=Venue.builder().name("QA Price Studio").neighbourhood("QA").timezone("Europe/Amsterdam").hourlyRateEur(new BigDecimal("999.99")).minimumBookingMinutes(999).fullDayRateEur(new BigDecimal("9999.99")).build();byte[] bytes=new FinancialDocumentPdfRenderer().bookingReceipt("QA-PRICE-RECEIPT",booking(),v);try(var pdf=org.apache.pdfbox.Loader.loadPDF(bytes)){String text=new org.apache.pdfbox.text.PDFTextStripper().getText(pdf);assertTrue(text.contains("EUR 19.99"));assertTrue(text.contains("EUR 20.32"));assertTrue(text.contains("EUR 500.00"));assertTrue(text.contains("61 minutes"));assertTrue(text.contains("1 minutes"));assertFalse(text.contains("999.99"));}}
 @ParameterizedTest @ValueSource(strings={"100.00","20.32","0.01","500.00"})
 @SuppressWarnings("unchecked") void providerReceivesExactEurDecimalStrings(String amount){var http=mock(RestTemplate.class);var props=new AppProperties(null,null,null,new AppProperties.MollieProperties("qa-not-a-real-key","http://localhost:3000/booked","",null,false,3600000,60000),null,null,null,null,null);when(http.exchange(anyString(),eq(HttpMethod.POST),any(HttpEntity.class),eq(MolliePaymentResponse.class))).thenReturn(ResponseEntity.ok(new MolliePaymentResponse("tr_qa","open",new MolliePaymentResponse.Amount(amount,"EUR"),null,Map.of())));new HttpMollieProviderClient(http,props).createPayment(UUID.randomUUID(),new BigDecimal(amount),UUID.randomUUID());var captor=ArgumentCaptor.forClass(HttpEntity.class);verify(http).exchange(anyString(),eq(HttpMethod.POST),captor.capture(),eq(MolliePaymentResponse.class));assertEquals(Map.of("currency","EUR","value",amount),((Map<?,?>)captor.getValue().getBody()).get("amount"));}
}
