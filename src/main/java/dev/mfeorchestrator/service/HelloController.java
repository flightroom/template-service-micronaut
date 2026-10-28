package dev.mfeorchestrator.service;

import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.serde.annotation.Serdeable;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Controller("/api/hello")
@Tag(name = "hello")
public class HelloController {

    @Serdeable
    public record HelloResponse(String message) {
    }

    @Get(produces = MediaType.APPLICATION_JSON)
    @Operation(summary = "Example endpoint")
    public HelloResponse hello() {
        return new HelloResponse("Hello from %serviceSlug%");
    }
}
