package com.neovarsity.toursattractions.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neovarsity.toursattractions.exception.BusinessException;
import com.neovarsity.toursattractions.exception.ResourceNotFoundException;
import com.neovarsity.wanderwise.common.WanderwiseConstants;
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

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(CONNECT_TIMEOUT)
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
        return exchange("GET", path, null, type, true);
    }

    private <T> T post(String path, Object payload, TypeReference<ApiResponse<T>> type) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            return exchange("POST", path, json, type, false);
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException("Catalog service unavailable");
        }
    }

    private <T> T exchange(String method, String path, String jsonBody,
                           TypeReference<ApiResponse<T>> type, boolean map404ToNotFound) {
        try {
            HttpResponse<String> response = send(method, path, jsonBody);
            if (map404ToNotFound && response.statusCode() == 404) {
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

    private HttpResponse<String> send(String method, String path, String jsonBody) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(catalogBaseUrl + path))
                .timeout(REQUEST_TIMEOUT)
                .header(WanderwiseConstants.INTERNAL_SECRET_HEADER, internalSecret);
        if ("POST".equals(method)) {
            builder.header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody != null ? jsonBody : "{}"));
        } else {
            builder.GET();
        }
        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }
}
