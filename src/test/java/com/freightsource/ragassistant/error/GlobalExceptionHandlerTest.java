package com.freightsource.ragassistant.error;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ProblemDetail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Mock
    private HttpServletRequest request;

    @Test
    void mapsIllegalArgumentExceptionToBadRequestWithTheMessage() {
        stubRequest("POST", "/ingest");

        ProblemDetail problem = handler.handleBadRequest(new IllegalArgumentException("Unsupported file type: x.zip"), request);

        assertThat(problem.getStatus()).isEqualTo(400);
        assertThat(problem.getDetail()).isEqualTo("Unsupported file type: x.zip");
    }

    @Test
    void mapsUnexpectedExceptionsToInternalServerErrorWithoutLeakingDetails() {
        stubRequest("GET", "/chat");

        ProblemDetail problem = handler.handleUnexpectedError(
                new RuntimeException("HTTP 401 - authentication_error: API key is invalid."), request);

        assertThat(problem.getStatus()).isEqualTo(500);
        assertThat(problem.getDetail())
                .doesNotContain("API key is invalid")
                .contains("server logs");
    }

    private void stubRequest(String method, String uri) {
        lenient().when(request.getMethod()).thenReturn(method);
        lenient().when(request.getRequestURI()).thenReturn(uri);
    }
}
