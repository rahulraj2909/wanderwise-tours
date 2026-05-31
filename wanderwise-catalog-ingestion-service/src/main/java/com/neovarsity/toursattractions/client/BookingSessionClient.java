package com.neovarsity.toursattractions.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class BookingSessionClient {

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @Value("${wanderwise.booking.base-url:http://localhost:8080}")
    private String bookingBaseUrl;

    public Optional<BookingUser> resolveUser(HttpServletRequest request) {
        String cookie = request.getHeader("Cookie");
        if (cookie == null || cookie.isBlank()) {
            return Optional.empty();
        }
        try {
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(bookingBaseUrl + "/api/v1/auth/me"))
                    .timeout(Duration.ofSeconds(10))
                    .header("Cookie", cookie)
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return Optional.empty();
            }
            JsonNode data = objectMapper.readTree(response.body()).get("data");
            if (data == null || data.isNull()) {
                return Optional.empty();
            }
            return Optional.of(BookingUser.builder()
                    .id(data.get("id").asLong())
                    .name(data.hasNonNull("name") ? data.get("name").asText() : "Customer")
                    .build());
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    @Builder
    public record BookingUser(Long id, String name) {
    }
}
