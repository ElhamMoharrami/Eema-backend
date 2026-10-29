package com.eema.backend.security;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.eema.backend.config.AppProperties;
import com.eema.backend.exception.GlobalExceptionHandler;
import com.eema.backend.mapper.UserMapper;
import com.eema.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.ServletWebRequest;
import java.util.Base64;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SecurityLoggingTest {
    private static final String MARKER = "QA_SECRET_DO_NOT_LOG";
    @Test void unexpectedProviderCauseDoesNotReachApplicationLogsOrResponse() {
        Logger logger=(Logger)LoggerFactory.getLogger(GlobalExceptionHandler.class);
        var captured=new ListAppender<ILoggingEvent>();captured.start();logger.addAppender(captured);
        try {
            var response=new GlobalExceptionHandler().handleUnexpected(new RuntimeException("provider wrapper",
                    new IllegalStateException(MARKER+"\r\nFORGED")),new ServletWebRequest(new MockHttpServletRequest()));
            assertFalse(response.getBody().toString().contains(MARKER));
            assertFalse(captured.list.isEmpty());
            for(var event:captured.list){assertFalse(event.getFormattedMessage().contains(MARKER));assertNull(event.getThrowableProxy(),"Do not attach an untrusted exception/cause with credentials");}
        } finally {logger.detachAppender(captured);captured.stop();}
    }
    @Test void malformedJwtHeaderCannotInjectTokenContentsIntoDebugLogs() throws Exception {
        var properties=new AppProperties(new AppProperties.JwtProperties(Base64.getEncoder().encodeToString("01234567890123456789012345678901".getBytes()),15,7),null,null,null,null,null,null,null,null);
        var jwt=new JwtService(properties);jwt.init();
        String token=Base64.getUrlEncoder().withoutPadding().encodeToString(("{\"alg\":\"HS256\",\"secret\":\""+MARKER+"\",BROKEN}").getBytes())+".e30.c2ln";
        Logger logger=(Logger)LoggerFactory.getLogger(JwtAuthenticationFilter.class);Level previous=logger.getLevel();logger.setLevel(Level.DEBUG);
        var captured=new ListAppender<ILoggingEvent>();captured.start();logger.addAppender(captured);
        try {
            var request=new MockHttpServletRequest();request.addHeader("Authorization","Bearer "+token);
            new JwtAuthenticationFilter(jwt,mock(UserRepository.class),mock(UserMapper.class)).doFilter(request,new MockHttpServletResponse(),(q,r)->{});
            assertFalse(captured.list.isEmpty());
            for(var event:captured.list){assertFalse(event.getFormattedMessage().contains(MARKER));assertNull(event.getThrowableProxy());}
        } finally {logger.setLevel(previous);logger.detachAppender(captured);captured.stop();}
    }
}
