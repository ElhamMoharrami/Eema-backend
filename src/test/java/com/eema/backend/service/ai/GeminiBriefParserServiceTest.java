package com.eema.backend.service.ai;

import com.eema.backend.entity.enums.ProductionActivity;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

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

        ParsedBrief parsed = new GeminiBriefParserService(client, mapper)
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

        new GeminiBriefParserService(client, new ObjectMapper().findAndRegisterModules())
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
}
