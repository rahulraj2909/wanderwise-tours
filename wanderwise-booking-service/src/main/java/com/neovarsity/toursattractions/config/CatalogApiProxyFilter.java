package com.neovarsity.toursattractions.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Set;

/**
 * Proxies catalog APIs to the ingestion service so the booking UI on :8080 stays same-origin.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class CatalogApiProxyFilter extends OncePerRequestFilter {

    private static final Set<String> PROXY_PREFIXES = Set.of(
            "/api/v1/attractions",
            "/api/v1/cities",
            "/api/v1/categories",
            "/api/v1/reviews"
    );

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Value("${wanderwise.catalog.base-url:http://localhost:8081}")
    private String catalogBaseUrl;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return PROXY_PREFIXES.stream().noneMatch(uri::startsWith);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String target = catalogBaseUrl + request.getRequestURI();
        String query = request.getQueryString();
        if (query != null && !query.isBlank()) {
            target += "?" + query;
        }

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(target))
                .timeout(Duration.ofSeconds(60));

        String method = request.getMethod();
        builder.method(method, bodyPublisher(request, method));
        copyRequestHeaders(request, builder);

        try {
            HttpResponse<InputStream> upstream = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofInputStream());
            response.setStatus(upstream.statusCode());
            upstream.headers().map().forEach((name, values) -> {
                if (isHopByHop(name)) {
                    return;
                }
                for (String value : values) {
                    response.addHeader(name, value);
                }
            });
            try (InputStream body = upstream.body()) {
                body.transferTo(response.getOutputStream());
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            response.sendError(HttpServletResponse.SC_BAD_GATEWAY, "Catalog service unavailable");
        } catch (Exception ex) {
            response.sendError(HttpServletResponse.SC_BAD_GATEWAY, "Catalog service unavailable");
        }
    }

    private static HttpRequest.BodyPublisher bodyPublisher(HttpServletRequest request, String method)
            throws IOException {
        if (HttpMethod.GET.matches(method) || HttpMethod.DELETE.matches(method) || HttpMethod.HEAD.matches(method)) {
            return HttpRequest.BodyPublishers.noBody();
        }
        byte[] body = request.getInputStream().readAllBytes();
        return body.length == 0 ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofByteArray(body);
    }

    private void copyRequestHeaders(HttpServletRequest request, HttpRequest.Builder builder) {
        var headerNames = request.getHeaderNames();
        if (headerNames == null) {
            return;
        }
        while (headerNames.hasMoreElements()) {
            String name = headerNames.nextElement();
            if (isHopByHop(name)) {
                continue;
            }
            request.getHeaders(name).asIterator().forEachRemaining(value -> builder.header(name, value));
        }
    }

    private static boolean isHopByHop(String header) {
        return header != null && Set.of("host", "connection", "content-length", "transfer-encoding")
                .contains(header.toLowerCase());
    }
}
