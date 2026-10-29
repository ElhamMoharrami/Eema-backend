package com.eema.backend.service.ai;

import com.eema.backend.entity.Venue;
import com.eema.backend.entity.enums.ProductionActivity;
import com.eema.backend.mapper.VenueMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class GeminiVenueRankerTest {

    @Test
    void rankingPromptReceivesAuthoritativeProductionFactsAndSafetyRules() {
        GeminiClient client = mock(GeminiClient.class);
        VenueMapper mapper = mock(VenueMapper.class);
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        Venue venue = mock(Venue.class);
        when(venue.getId()).thenReturn(UUID.randomUUID());
        when(venue.getName()).thenReturn("Sound Stage");
        when(venue.getHourlyRateEur()).thenReturn(new BigDecimal("175.00"));
        when(venue.getMinimumBookingMinutes()).thenReturn(240);
        when(venue.getMaxCrewSize()).thenReturn(30);
        when(venue.getPermittedActivities()).thenReturn(List.of(ProductionActivity.INTERVIEW));
        when(venue.getNaturalLight()).thenReturn(false);
        when(venue.getSoundRecordingSuitable()).thenReturn(true);
        when(venue.getProductionRules()).thenReturn("No smoke effects");
        when(venue.getPowerDetails()).thenReturn("3-phase power available");
        when(venue.getTags()).thenReturn(List.of("studio"));
        when(client.generateContent(anyString())).thenReturn("raw");
        when(client.extractGeneratedText("raw")).thenReturn("[]");

        ParsedBrief brief = ParsedBriefFixtures.fromJson("""
                {"requestedDate":"2026-08-25","requestedStartTime":"10:00:00",
                 "requestedEndTime":"16:00:00"}
                """);
        new GeminiVenueRanker(client, objectMapper, mapper)
                .rank("Interview", brief, List.of(venue));

        ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
        verify(client).generateContent(prompt.capture());
        assertThat(prompt.getValue())
                .contains("already passed all deterministic hard requirements")
                .contains("\"maxCrewSize\":30")
                .contains("\"permittedActivities\":[\"INTERVIEW\"]")
                .contains("\"soundRecordingSuitable\":true")
                .contains("No smoke effects")
                .contains("3-phase power available")
                .contains("Do not claim current availability")
                .contains("available for the exact requested interval 2026-08-25 10:00-16:00")
                .contains("Do not judge or claim a total budget fit")
                .contains("Do not claim excellent acoustics");
        assertThat(prompt.getValue()).doesNotContain("ownerEmail");
    }
}
