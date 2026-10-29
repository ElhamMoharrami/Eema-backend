package com.eema.backend.service;

import com.eema.backend.entity.*;
import com.eema.backend.entity.enums.*;
import com.eema.backend.exception.ConflictException;
import com.eema.backend.repository.*;
import com.eema.backend.support.ReadyVenueFixture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.context.ApplicationEventPublisher;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class VenueReadinessContractTest {
    final VenueProductionProfileValidator validator = new VenueProductionProfileValidator();
    static Stream<Arguments> missing() {
        return Stream.concat(ReadyVenueFixture.KEYS.stream().map(k -> Arguments.of(k, null)), Stream.of(
                Arguments.of("permittedActivities", List.of()), Arguments.of("productionRules", ""),
                Arguments.of("productionRules", " \t\n"), Arguments.of("timezone", ""),
                Arguments.of("timezone", " \t"), Arguments.of("photoPaths", List.of())));
    }
    static Map<String,Object> snapshot(Venue v) throws Exception {
        Map<String,Object> values = new HashMap<>();
        for (var field : Venue.class.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) continue;
            field.setAccessible(true); Object value = field.get(v);
            values.put(field.getName(), value instanceof List<?> list ? new ArrayList<>(list) : value);
        }
        return values;
    }
    @Test void canonicalFixtureIsReadyWithAllFourBooleansFalseAndOptionalFactsAbsent() {
        Venue v = ReadyVenueFixture.complete();
        assertTrue(validator.readinessIssues(v).isEmpty());
        for(String field : List.of("blackoutPossible", "parkingAvailable", "liftAvailable", "wheelchairAccessible",
                "ceilingHeightM", "powerDetails", "fullDayRateEur", "clipUrl", "prohibitedActivities"))
            assertNull(new BeanWrapperImpl(v).getPropertyValue(field), field);
        v.setPhotoPaths(List.of(v.getPhotoPaths().getFirst(), "/api/upload/files/22222222-2222-4222-8222-222222222222.png"));
        assertTrue(validator.readinessIssues(v).isEmpty());
    }
    @ParameterizedTest @MethodSource("missing")
    void exactSingleIssueRestoresAndEvaluationNeverMutates(String key, Object missing) throws Exception {
        Venue v = ReadyVenueFixture.complete(); assertTrue(validator.readinessIssues(v).isEmpty());
        var properties = new BeanWrapperImpl(v); Object original = properties.getPropertyValue(key);
        properties.setPropertyValue(key, missing); var before = snapshot(v);
        assertEquals(List.of(key), validator.readinessIssues(v));
        assertEquals(before, snapshot(v));
        properties.setPropertyValue(key, original); assertTrue(validator.readinessIssues(v).isEmpty());
    }
    @Test void completeIssueKeySetIsOrderedAccumulatedImmutableAndPure() throws Exception {
        Venue v = ReadyVenueFixture.complete(); var properties = new BeanWrapperImpl(v);
        ReadyVenueFixture.KEYS.forEach(k -> properties.setPropertyValue(k, null)); var before = snapshot(v);
        var issues = validator.readinessIssues(v); assertEquals(ReadyVenueFixture.KEYS, issues);
        assertThrows(UnsupportedOperationException.class, () -> issues.add("invented"));
        assertEquals(before, snapshot(v));
    }
    @ParameterizedTest @ValueSource(strings={"naturalLight","loadingAccess","bathroomsAvailable","soundRecordingSuitable"})
    void TrueAndFalseAreBothAnswered(String key) {
        Venue v=ReadyVenueFixture.complete();var properties=new BeanWrapperImpl(v);
        for(boolean value:List.of(false,true)) {properties.setPropertyValue(key,value);assertTrue(validator.readinessIssues(v).isEmpty());}
    }
    @ParameterizedTest @MethodSource("missing")
    void everyMissingConditionGatesAllThreeServiceTransitionsWithoutEffects(String key,Object missing) throws Exception {
        for(VenueStatus source:List.of(VenueStatus.DRAFT,VenueStatus.PENDING,VenueStatus.SUSPENDED)) {
            var repository=mock(VenueRepository.class);var current=mock(CurrentUserService.class);
            var verification=mock(OwnerVerificationService.class);var audit=mock(MarketplaceTrustAuditService.class);
            var events=mock(ApplicationEventPublisher.class);var policies=mock(PolicyAcceptanceService.class);
            var service=new VenueModerationService(repository,current,verification,validator,audit,
                    mock(MarketplaceTrustEventRepository.class),events,policies);
            User owner=User.builder().id(UUID.randomUUID()).role(UserRole.OWNER).active(true).emailVerified(true).build();
            User admin=User.builder().id(UUID.randomUUID()).role(UserRole.ADMIN).active(true).build();
            Venue v=ReadyVenueFixture.complete();v.setOwner(owner);v.setStatus(source);
            when(repository.findByIdForUpdate(v.getId())).thenReturn(Optional.of(v));
            when(current.getCurrentUser()).thenReturn(source==VenueStatus.DRAFT?owner:admin);
            when(verification.isVerified(owner.getId())).thenReturn(true);
            var properties=new BeanWrapperImpl(v);Object original=properties.getPropertyValue(key);properties.setPropertyValue(key,missing);
            var before=snapshot(v);
            Runnable transition=()->{switch(source){case DRAFT->service.submit(v.getId());case PENDING->service.approve(v.getId(),null);case SUSPENDED->service.reactivate(v.getId(),null);default->throw new AssertionError();}};
            assertEquals("Venue is not production-ready: "+key,assertThrows(ConflictException.class,transition::run).getMessage());
            assertEquals(before,snapshot(v));verifyNoInteractions(audit,events);verify(repository,never()).save(any());
            properties.setPropertyValue(key,original);transition.run();
            assertEquals(source==VenueStatus.DRAFT?VenueStatus.PENDING:VenueStatus.ACTIVE,v.getStatus());
            verify(events).publishEvent(any(com.eema.backend.event.TrustNotificationEvent.class));
        }
    }
    @Test void numericValidityIsSeparateFromPresenceReadiness() {
        var complete=ReadyVenueFixture.complete();var properties=new BeanWrapperImpl(complete);
        Map<String,Object> baseline=new HashMap<>();
        for(var field:com.eema.backend.dto.request.UpdateVenueRequest.class.getRecordComponents())
            baseline.put(field.getName(),properties.getPropertyValue(field.getName()));
        var mapper=new com.fasterxml.jackson.databind.ObjectMapper();
        try(var factory=jakarta.validation.Validation.buildDefaultValidatorFactory()) {
            var dtoValidator=factory.getValidator();
            for(var entry:Map.of("hourlyRateEur",List.of(-1,0,0.01,100),
                    "minimumBookingMinutes",List.of(-1,0,1,10080,10081),
                    "minimumLeadTimeMinutes",List.of(-1,0,59,60,43200,43201),
                    "maxCrewSize",List.of(-1,0,1,10000,10001)).entrySet()) {
                String key=entry.getKey();
                List<Object> values=new ArrayList<>(entry.getValue());values.add(null);
                for(Object value:values) {
                    var body=new HashMap<>(baseline);body.put(key,value);
                    var dto=mapper.convertValue(body,com.eema.backend.dto.request.UpdateVenueRequest.class);
                    boolean invalid=value==null?!key.equals("maxCrewSize"):
                            ((Number)value).doubleValue()<(key.equals("hourlyRateEur")?0.01:key.equals("minimumLeadTimeMinutes")?60:1)
                            || ((Number)value).doubleValue()>(key.equals("minimumBookingMinutes")?10080:key.equals("minimumLeadTimeMinutes")?43200:key.equals("maxCrewSize")?10000:99999999.99);
                    assertEquals(invalid,dtoValidator.validate(dto).stream().anyMatch(v->v.getPropertyPath().toString().equals(key)),key+"="+value);
                }
            }
        }
        complete.setHourlyRateEur(java.math.BigDecimal.ZERO);complete.setMinimumBookingMinutes(0);
        complete.setMinimumLeadTimeMinutes(59);complete.setMaxCrewSize(0);
        // Internal object only: numeric invalidity is rejected before workflow by DTO/entity/DB validation.
        assertTrue(validator.readinessIssues(complete).isEmpty());
    }

}
