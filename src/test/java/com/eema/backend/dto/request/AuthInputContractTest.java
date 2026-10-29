package com.eema.backend.dto.request;

import com.eema.backend.config.JacksonConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonMappingException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.context.annotation.Import;

import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

@JsonTest
@Import(JacksonConfig.class)
class AuthInputContractTest {
    @Autowired ObjectMapper json;

    @Test void strictnessIsScopedToAllSixAuthDtos() throws Exception {
        Map<Class<?>, String> bodies = Map.of(
                RegisterRequest.class, "{\"email\":\"a@example.test\",\"password\":\" password \",\"fullName\":\" QA  Scout \",\"role\":\"SCOUT\"}",
                LoginRequest.class, "{\"email\":\"a@example.test\",\"password\":\" password \"}",
                RefreshRequest.class, "{\"refreshToken\":\" Mixed-Token \"}",
                VerifyEmailRequest.class, "{\"token\":\" Mixed-Token \"}",
                EmailAddressRequest.class, "{\"email\":\"a@example.test\"}",
                ResetPasswordRequest.class, "{\"token\":\" Mixed-Token \",\"newPassword\":\" password \"}");
        for (var entry : bodies.entrySet()) {
            assertNotNull(json.readValue(entry.getValue(), entry.getKey()));
            String extra = entry.getValue().replaceFirst("\\{", "{\"unexpected\":true,");
            assertThrows(JsonMappingException.class, () -> json.readValue(extra, entry.getKey()));
        }
        assertEquals("ok", json.readValue("{\"value\":\"ok\",\"extra\":true}", OrdinaryRequest.class).value());
    }
    record OrdinaryRequest(String value) {}

    @Test void bindingNormalizesHumanInputButPreservesSecrets() throws Exception {
        var register = json.readValue("{\"email\":\"  Scout.A+RUN@Example.Test  \",\"password\":\" password \",\"fullName\":\"  QA  Scout  \",\"role\":\"SCOUT\"}", RegisterRequest.class);
        assertEquals("scout.a+run@example.test", register.email()); assertEquals("QA  Scout", register.fullName());
        assertTrue(" password ".equals(register.password()));
        assertTrue(" Mixed-Token ".equals(json.readValue("{\"refreshToken\":\" Mixed-Token \"}", RefreshRequest.class).refreshToken()));
        assertTrue(" Mixed-Token ".equals(json.readValue("{\"token\":\" Mixed-Token \"}", VerifyEmailRequest.class).token()));
        var reset = json.readValue("{\"token\":\" Mixed-Token \",\"newPassword\":\" password \"}", ResetPasswordRequest.class);
        assertTrue(" Mixed-Token ".equals(reset.token())); assertTrue(" password ".equals(reset.newPassword()));
        assertEquals("scout @example.test", EmailNormalizer.normalize(" Scout @Example.Test "));
        assertNull(EmailNormalizer.normalize(null));
    }
}
