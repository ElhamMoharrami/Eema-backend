package com.eema.backend.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class RequestCorrelationFilterTest {
    private final RequestCorrelationFilter filter = new RequestCorrelationFilter();

    @Test void propagatesSafeRequestId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(RequestCorrelationFilter.HEADER, "edge-request_123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, (req, res) -> assertThat(org.slf4j.MDC.get("requestId")).isEqualTo("edge-request_123"));
        assertThat(response.getHeader(RequestCorrelationFilter.HEADER)).isEqualTo("edge-request_123");
        assertThat(org.slf4j.MDC.get("requestId")).isNull();
    }

    @Test void replacesUnsafeRequestId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(RequestCorrelationFilter.HEADER, "bad header\nvalue");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, (req, res) -> {});
        assertThat(response.getHeader(RequestCorrelationFilter.HEADER)).matches("[0-9a-f-]{36}");
    }
}
