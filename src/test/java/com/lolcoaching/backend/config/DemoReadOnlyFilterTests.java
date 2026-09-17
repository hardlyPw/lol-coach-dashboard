package com.lolcoaching.backend.config;

import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class DemoReadOnlyFilterTests {
    @ParameterizedTest
    @ValueSource(strings = {"POST", "PUT", "PATCH", "DELETE", "TRACE"})
    void rejectsMutationsBeforeTheyReachTheController(String method) throws Exception {
        var reached = new AtomicBoolean();
        var response = new MockHttpServletResponse();
        new DemoReadOnlyFilter().doFilter(new MockHttpServletRequest(method, "/api/matches/1"),
            response, (req, res) -> reached.set(true));
        assertEquals(403, response.getStatus());
        assertFalse(reached.get());
    }

    @ParameterizedTest
    @ValueSource(strings = {"GET", "HEAD", "OPTIONS"})
    void permitsReadRequests(String method) throws Exception {
        var reached = new AtomicBoolean();
        new DemoReadOnlyFilter().doFilter(new MockHttpServletRequest(method, "/api/matches/list"),
            new MockHttpServletResponse(), (req, res) -> reached.set(true));
        assertTrue(reached.get());
    }
}
