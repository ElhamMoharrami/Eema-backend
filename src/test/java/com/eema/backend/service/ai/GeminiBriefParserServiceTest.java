package com.eema.backend.service.ai;

import com.eema.backend.entity.enums.ProductionActivity;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class GeminiBriefParserServiceTest {

    @Test
    void mapsStructuredProductionRequirementsWithoutLiveGemini() {
        GeminiClient client = mock(GeminiClient.class);
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        String json = """
                {
                  "productionActivity": "INTERVIEW",
                  "numPeople": 6,
                  "requiresSoundRecordingSuitable": true,
                  "prefersNaturalLight": true,
                  "minimumCeilingHeightM": 3.5,
                  "requiresParking": true,
                  "minimumParkingSpaces": 2,
                  "mustHaveTags": ["studio"],
                  "confidenceScore": 0.93
                }
                """;
        when(client.generateContent(anyString())).thenReturn("raw");
        when(client.extractGeneratedText("raw")).thenReturn(json);

        MatchingReferenceDateProvider dates = mock(MatchingReferenceDateProvider.class);
        when(dates.today()).thenReturn(LocalDate.of(2026, 8, 20));
        ParsedBrief parsed = new GeminiBriefParserService(client, mapper, dates)
                .parseBrief("Interview for six people, needs clean audio");

        assertThat(parsed.productionActivity()).isEqualTo(ProductionActivity.INTERVIEW);
        assertThat(parsed.numPeople()).isEqualTo(6);
        assertThat(parsed.requiresSoundRecordingSuitable()).isTrue();
        assertThat(parsed.prefersNaturalLight()).isTrue();
        assertThat(parsed.minimumCeilingHeightM()).isEqualByComparingTo("3.5");
        assertThat(parsed.minimumParkingSpaces()).isEqualTo(2);
        assertThat(parsed.mustHaveTags()).containsExactly("studio");
    }

    @Test
    void promptDefinesRequirementStrengthEnumsAndStructuredTagBoundary() {
        GeminiClient client = mock(GeminiClient.class);
        when(client.generateContent(anyString())).thenReturn("raw");
        when(client.extractGeneratedText("raw")).thenReturn("{}");

        MatchingReferenceDateProvider dates = mock(MatchingReferenceDateProvider.class);
        when(dates.today()).thenReturn(LocalDate.of(2026, 8, 20));
        new GeminiBriefParserService(client, new ObjectMapper().findAndRegisterModules(), dates)
                .parseBrief("Industrial warehouse, parking would be nice");

        ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
        verify(client).generateContent(prompt.capture());
        assertThat(prompt.getValue())
                .contains("Requirement strength")
                .contains("PHOTO, FILM, VIDEO, INTERVIEW")
                .contains("COMMERCIAL, MUSIC_VIDEO, CONTENT_CREATION")
                .contains("Never put natural light")
                .contains("If ambiguous, return null")
                .contains("parking would be nice");
    }

    @Test
    void relativeDateUsesExplicitApplicationReferenceDate() {
        GeminiClient client = mock(GeminiClient.class);
        MatchingReferenceDateProvider dates = mock(MatchingReferenceDateProvider.class);
        when(dates.today()).thenReturn(LocalDate.of(2026, 8, 20));
        when(client.generateContent(anyString())).thenReturn("raw");
        when(client.extractGeneratedText("raw")).thenReturn("""
                {"requestedDate":"2026-08-21","requestedStartTime":"10:00:00",
                 "requestedEndTime":"16:00:00","timingDatePreferences":"tomorrow 10am-4pm"}
                """);

        ParsedBrief parsed = new GeminiBriefParserService(
                client, new ObjectMapper().findAndRegisterModules(), dates
        ).parseBrief("Tomorrow from 10am to 4pm");

        assertThat(parsed.requestedDate()).isEqualTo(LocalDate.of(2026, 8, 21));
        assertThat(parsed.requestedStartTime()).hasToString("10:00");
        assertThat(parsed.requestedEndTime()).hasToString("16:00");
        ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
        verify(client).generateContent(prompt.capture());
        assertThat(prompt.getValue()).contains("Current date: 2026-08-20")
                .contains("Europe/Amsterdam");
    }

    @Test
    void ambiguousTimingRemainsNonExactContext() {
        GeminiClient client = mock(GeminiClient.class);
        MatchingReferenceDateProvider dates = mock(MatchingReferenceDateProvider.class);
        when(dates.today()).thenReturn(LocalDate.of(2026, 8, 20));
        when(client.generateContent(anyString())).thenReturn("raw");
        when(client.extractGeneratedText("raw")).thenReturn("""
                {"requestedDate":null,"requestedStartTime":null,"requestedEndTime":null,
                 "timingDatePreferences":"Friday afternoon"}
                """);

        ParsedBrief parsed = new GeminiBriefParserService(
                client, new ObjectMapper().findAndRegisterModules(), dates
        ).parseBrief("Friday afternoon");

        assertThat(parsed.requestedDate()).isNull();
        assertThat(parsed.requestedStartTime()).isNull();
        assertThat(parsed.requestedEndTime()).isNull();
        assertThat(parsed.timingDatePreferences()).isEqualTo("Friday afternoon");
    }
}
