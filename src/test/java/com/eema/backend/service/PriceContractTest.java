package com.eema.backend.service;

import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.math.*;
import java.time.*;
import java.util.*;
import java.util.stream.*;
import static org.junit.jupiter.api.Assertions.*;

class PriceContractTest {
 final BookingPriceCalculator calculator=new BookingPriceCalculator();
 static Stream<Arguments> cases()throws Exception{var tree=new ObjectMapper().readTree(PriceContractTest.class.getResourceAsStream("/pricing-cases.json"));return StreamSupport.stream(tree.spliterator(),false).map(n->Arguments.of(n.path("id").asText(),n));}
 @ParameterizedTest(name="{0}") @MethodSource("cases")
 void sharedDecimalContract(String id,JsonNode n){int d=n.path("duration").asInt();var q=calculator.calculate(new BigDecimal(n.path("hourly").asText()),n.path("minimum").asInt(),n.path("cap").isNull()?null:new BigDecimal(n.path("cap").asText()),LocalTime.MIDNIGHT,LocalTime.MIDNIGHT.plusMinutes(d));assertEquals(new BigDecimal(n.path("expected").asText()),q.finalPriceEur());assertEquals(new BigDecimal(n.path("hourlyTotal").asText()),q.calculatedHourlyTotal());assertEquals(d,q.durationMinutes());assertEquals(n.path("billable").asInt(),q.billableMinutes());}
 @ParameterizedTest @ValueSource(strings={"hourly-zero","hourly-negative","minimum-zero","minimum-negative","cap-zero","cap-negative","equal","reverse","overnight","subminute"})
 void invalidInputsDoNotProduceBookingPrices(String kind){BigDecimal rate=BigDecimal.ONE,cap=null;int min=1;LocalTime start=LocalTime.NOON,end=start.plusHours(1);switch(kind){case "hourly-zero"->rate=BigDecimal.ZERO;case "hourly-negative"->rate=BigDecimal.ONE.negate();case "minimum-zero"->min=0;case "minimum-negative"->min=-1;case "cap-zero"->cap=BigDecimal.ZERO;case "cap-negative"->cap=BigDecimal.ONE.negate();case "equal"->end=start;case "reverse"->end=start.minusHours(1);case "overnight"->{start=LocalTime.of(23,0);end=LocalTime.of(2,0);}case "subminute"->end=start.plusSeconds(30);}final var r=rate;final var c=cap;final int m=min;final var s=start;final var e=end;Class<? extends RuntimeException> expected=List.of("equal","reverse","overnight").contains(kind)?com.eema.backend.exception.BadRequestException.class:IllegalArgumentException.class;assertThrows(expected,()->calculator.calculate(r,m,c,s,e));}
 @Test void lightweightMonotonicityMinimumAndCapInvariants(){for(String rate:List.of("0.01","0.30","19.99","125.50"))for(int min:List.of(1,60,240))for(String cap:List.of("0.01","200","500")){BigDecimal previous=BigDecimal.ZERO;for(int d=1;d<=600;d++){var q=calculator.calculate(new BigDecimal(rate),min,new BigDecimal(cap),LocalTime.MIDNIGHT,LocalTime.MIDNIGHT.plusMinutes(d));assertTrue(q.billableMinutes()>=d&&q.billableMinutes()>=min);assertTrue(q.finalPriceEur().compareTo(previous)>=0);assertTrue(q.finalPriceEur().compareTo(new BigDecimal(cap))<=0);previous=q.finalPriceEur();}}}
 @ParameterizedTest @ValueSource(strings={"2026-03-29","2026-10-25"})
 void dstUsesSelectedWallMinutesNotElapsedInstants(String date){var start=LocalTime.of(1,30);var end=LocalTime.of(3,30);long elapsed=Duration.between(LocalDate.parse(date).atTime(start).atZone(ZoneId.of("Europe/Amsterdam")),LocalDate.parse(date).atTime(end).atZone(ZoneId.of("Europe/Amsterdam"))).toMinutes();assertNotEquals(120,elapsed);assertEquals(120,calculator.calculate(new BigDecimal("100"),1,null,start,end).durationMinutes());}
 @Test void roundsHourlyBeforeComparingNormalizedCap(){var q=calculator.calculate(new BigDecimal("0.30"),1,new BigDecimal("0.006"),LocalTime.NOON,LocalTime.NOON.plusMinutes(1));assertEquals(new BigDecimal("0.01"),q.calculatedHourlyTotal());assertEquals(new BigDecimal("0.01"),q.fullDayRateEur());assertEquals(new BigDecimal("0.01"),q.finalPriceEur());}
 @Test void technicalAmountBoundIsAppliedAfterCap(){
  var rate=new BigDecimal("99999999.99");
  assertEquals(rate,calculator.calculate(rate,60,null,LocalTime.NOON,LocalTime.of(13,0)).finalPriceEur());
  assertThrows(IllegalArgumentException.class,()->calculator.calculate(rate,10080,null,LocalTime.NOON,LocalTime.of(13,0)));
  assertEquals(new BigDecimal("500.00"),calculator.calculate(rate,10080,new BigDecimal("500"),LocalTime.NOON,LocalTime.of(13,0)).finalPriceEur());
 }
}
