package com.eema.backend.ai;

import com.eema.backend.service.ai.GeminiProperties;
import com.eema.backend.service.VenueFilterBuilder;
import com.eema.backend.entity.Venue;
import com.eema.backend.repository.VenueRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AiServiceImplTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private GeminiProperties geminiProperties;

    @Mock
    private VenueRepository venueRepository;

    @Mock
    private VenueFilterBuilder venueFilterBuilder;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private AiServiceImpl aiService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testConvertToServiceFormat() {
        // Arrange
        com.eema.backend.ai.ParsedBrief aiBrief = new com.eema.backend.ai.ParsedBrief(
                new BigDecimal("1000"),   // budgetMinEur
                new BigDecimal("2000"),   // budgetMaxEur
                100,                      // sizeM2Min
                200,                      // sizeM2Max
                "Jordaan",                // neighbourhood
                java.util.List.of("De Pijp"),       // neighbourhoodOptions
                java.util.List.of("warehouse"),     // mustHaveTags
                java.util.List.of("industrial"),    // niceToHaveTags
                "industrial",             // aesthetic
                "directional",            // lighting
                "product shoot",          // purpose
                "test brief",             // briefSummary
                0.8                       // confidenceScore
        );

        // Act
        com.eema.backend.service.ai.ParsedBrief serviceBrief = aiService.convertToServiceFormat(aiBrief);

        // Assert
        assertNotNull(serviceBrief);
        assertEquals(new BigDecimal("2000"), serviceBrief.budgetPerDay()); // Should use max budget
        assertEquals("Jordaan", serviceBrief.neighbourhood()); // Should use neighbourhoodOptions[0]
        assertEquals(100, serviceBrief.sizeM2()); // Should use sizeM2Min
        assertEquals(java.util.List.of("warehouse", "industrial"), serviceBrief.tags()); // Combined tags
        assertEquals("industrial", serviceBrief.aesthetic());
        assertEquals("product shoot", serviceBrief.shootType());
        assertEquals("directional", serviceBrief.lightingPreferences());
        assertEquals(0.8, serviceBrief.confidenceScore());
    }

    @Test
    void testConvertToServiceFormatWithNullValues() {
        // Arrange
        com.eema.backend.ai.ParsedBrief aiBrief = new com.eema.backend.ai.ParsedBrief(
                null,                     // budgetMinEur
                null,                     // budgetMaxEur
                null,                     // sizeM2Min
                null,                     // sizeM2Max
                null,                     // neighbourhood
                null,                     // neighbourhoodOptions
                null,                     // mustHaveTags
                null,                     // niceToHaveTags
                null,                     // aesthetic
                null,                     // lighting
                null,                     // purpose
                null,                     // briefSummary
                null                      // confidenceScore
        );

        // Act
        com.eema.backend.service.ai.ParsedBrief serviceBrief = aiService.convertToServiceFormat(aiBrief);

        // Assert
        assertNotNull(serviceBrief);
        assertNull(serviceBrief.budgetPerDay());
        assertNull(serviceBrief.neighbourhood());
        assertNull(serviceBrief.sizeM2());
        assertTrue(serviceBrief.tags().isEmpty());
        assertNull(serviceBrief.aesthetic());
        assertNull(serviceBrief.shootType());
        assertNull(serviceBrief.lightingPreferences());
        assertNull(serviceBrief.accessibility());
        assertNull(serviceBrief.timingDatePreferences());
        assertNull(serviceBrief.confidenceScore());
    }
}
