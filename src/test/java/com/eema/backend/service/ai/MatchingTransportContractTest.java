package com.eema.backend.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.*;
import org.springframework.web.client.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MatchingTransportContractTest {
    final RestTemplate http=mock(RestTemplate.class);
    GeminiClient client(){var p=new GeminiProperties();p.setApiKey("QA_NOT_A_REAL_KEY");return new GeminiClient(p,http,new ObjectMapper());}
    @Test void keyUsesHeaderAndFencesAreStripped()throws Exception{
        when(http.exchange(anyString(),eq(HttpMethod.POST),any(HttpEntity.class),eq(String.class))).thenReturn(ResponseEntity.ok("QA"));
        client().generateContent("QA brief data");var url=org.mockito.ArgumentCaptor.forClass(String.class);var entity=org.mockito.ArgumentCaptor.forClass(HttpEntity.class);verify(http).exchange(url.capture(),eq(HttpMethod.POST),entity.capture(),eq(String.class));
        assertFalse(url.getValue().contains("key="));assertFalse(url.getValue().contains("QA_NOT_A_REAL_KEY"));assertEquals("QA_NOT_A_REAL_KEY",entity.getValue().getHeaders().getFirst("x-goog-api-key"));
        String body=new ObjectMapper().writeValueAsString(entity.getValue().getBody());assertFalse(body.contains("QA_NOT_A_REAL_KEY"));assertTrue(body.contains("application/json"));
        for(String text:List.of("{}","```json\n{}\n```","```\n{}\n```")){
            String response=new ObjectMapper().writeValueAsString(Map.of("candidates",List.of(Map.of("content",Map.of("parts",List.of(Map.of("text",text)))))));
            assertEquals("{}",client().extractGeneratedText(response));
        }
    }
    @ParameterizedTest @ValueSource(strings={"","null","not json","{}","{\"candidates\":[]}","{\"candidates\":[{\"content\":{\"parts\":[]}}]}","{\"candidates\":[{\"content\":{\"parts\":[{\"text\":42}]}}]}"})
    void invalidEnvelope(String response){assertThrows(AiServiceException.class,()->client().extractGeneratedText(response));}
    @ParameterizedTest @ValueSource(ints={400,429,500,503,0}) void transportFailuresAndRetryBound(int status){
        RuntimeException failure=status==0?new ResourceAccessException("QA timeout"):HttpClientErrorException.create(HttpStatusCode.valueOf(status),"QA provider failure",HttpHeaders.EMPTY,new byte[0],null);
        when(http.exchange(anyString(),eq(HttpMethod.POST),any(HttpEntity.class),eq(String.class))).thenThrow(failure);
        assertThrows(AiServiceException.class,()->client().generateContent("QA"));verify(http,times(status==400?1:3)).exchange(anyString(),eq(HttpMethod.POST),any(HttpEntity.class),eq(String.class));
    }
}
