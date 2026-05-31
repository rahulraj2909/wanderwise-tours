package com.neovarsity.toursattractions.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neovarsity.toursattractions.exception.BusinessException;
import com.neovarsity.toursattractions.exception.ResourceNotFoundException;
import com.neovarsity.wanderwise.common.dto.ApiResponse;
import com.neovarsity.wanderwise.common.dto.CatalogAttractionDto;
import com.neovarsity.wanderwise.common.dto.CatalogPaxTypeDto;
import com.neovarsity.wanderwise.common.dto.ReserveSeatsRequest;
import com.neovarsity.wanderwise.common.dto.SlotActionResultDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CatalogClient {

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Value("${wanderwise.catalog.base-url:http://localhost:8081}")
    private String catalogBaseUrl;

    @Value("${wanderwise.internal-secret:wanderwise-internal}")
    private String internalSecret;

    public CatalogAttractionDto getAttraction(Long id) {
        return get("/internal/v1/attractions/" + id, new TypeReference<ApiResponse<CatalogAttractionDto>>() {
        });
    }

    public List<CatalogPaxTypeDto> listPaxTypes(Long attractionId) {
        return get("/internal/v1/attractions/" + attractionId + "/pax-types",
                new TypeReference<ApiResponse<List<CatalogPaxTypeDto>>>() {
                });
    }

    public CatalogPaxTypeDto findPaxByCode(Long attractionId, String code) {
        return listPaxTypes(attractionId).stream()
                .filter(p -> p.code().equalsIgnoreCase(code))
                .findFirst()
                .orElseThrow(() -> new BusinessException("Invalid pax type: " + code));
    }

    public void reserveSeats(Long slotId, int guests) {
        SlotActionResultDto result = post(
                "/internal/v1/time-slots/" + slotId + "/reserve",
                reserveBody(guests),
                new TypeReference<ApiResponse<SlotActionResultDto>>() {
                });
        if (!result.success()) {
            throw new BusinessException(result.message() != null ? result.message() : "Could not reserve seats");
        }
    }

    public void releaseSeats(Long slotId, int guests) {
        if (slotId == null) {
            return;
        }
        post("/internal/v1/time-slots/" + slotId + "/release", reserveBody(guests),
                new TypeReference<ApiResponse<SlotActionResultDto>>() {
                });
    }

    private ReserveSeatsRequest reserveBody(int guests) {
        ReserveSeatsRequest req = new ReserveSeatsRequest();
        req.setGuests(guests);
        return req;
    }

    private <T> T get(String path, TypeReference<ApiResponse<T>> type) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(catalogBaseUrl + path))
                    .timeout(Duration.ofSeconds(30))
                    .header("X-Internal-Secret", internalSecret)
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 404) {
                throw new ResourceNotFoundException("Catalog resource not found");
            }
            if (response.statusCode() >= 400) {
                throw new BusinessException("Catalog service error: " + response.statusCode());
            }
            ApiResponse<T> body = objectMapper.readValue(response.body(), type);
            return body.getData();
        } catch (BusinessException | ResourceNotFoundException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException("Catalog service unavailable");
        }
    }

    private <T> T post(String path, Object payload, TypeReference<ApiResponse<T>> type) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(catalogBaseUrl + path))
                    .timeout(Duration.ofSeconds(30))
                    .header("X-Internal-Secret", internalSecret)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new BusinessException("Catalog service error: " + response.statusCode());
            }
            ApiResponse<T> body = objectMapper.readValue(response.body(), type);
            return body.getData();
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException("Catalog service unavailable");
        }
    }
}
