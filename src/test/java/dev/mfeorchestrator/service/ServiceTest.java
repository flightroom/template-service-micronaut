package dev.mfeorchestrator.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import java.util.Map;
import org.junit.jupiter.api.Test;

@MicronautTest
class ServiceTest {

    @Inject
    @Client("/")
    HttpClient client;

    @Test
    void helloReturnsAMessage() {
        Map<?, ?> body = client.toBlocking().retrieve(HttpRequest.GET("/api/hello"), Map.class);
        assertNotNull(body.get("message"));
    }

    @Test
    void livenessIsUp() {
        Map<?, ?> body = client.toBlocking().retrieve(HttpRequest.GET("/health/liveness"), Map.class);
        assertEquals("UP", body.get("status"));
    }

    @Test
    void readinessIsUp() {
        Map<?, ?> body = client.toBlocking().retrieve(HttpRequest.GET("/health/readiness"), Map.class);
        assertEquals("UP", body.get("status"));
    }

    @Test
    void openApiJsonContainsHello() {
        String spec = client.toBlocking().retrieve(HttpRequest.GET("/swagger/openapi.json"), String.class);
        assertTrue(spec.contains("\"/api/hello\""));
    }

    @Test
    void swaggerUiIsServed() {
        String html = client.toBlocking().retrieve(HttpRequest.GET("/swagger-ui/index.html"), String.class);
        assertTrue(html.contains("<html"));
    }

    @Test
    void unknownPathIs404() {
        HttpClientResponseException e = org.junit.jupiter.api.Assertions.assertThrows(
            HttpClientResponseException.class, () -> client.toBlocking().exchange(HttpRequest.GET("/nope")));
        assertEquals(HttpStatus.NOT_FOUND, e.getStatus());
    }
}
