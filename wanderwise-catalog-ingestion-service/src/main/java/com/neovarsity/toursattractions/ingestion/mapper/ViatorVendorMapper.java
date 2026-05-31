package com.neovarsity.toursattractions.ingestion.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neovarsity.toursattractions.entity.enums.AttractionType;
import com.neovarsity.toursattractions.ingestion.VendorProductMapper;
import com.neovarsity.toursattractions.ingestion.model.CanonicalActivity;
import com.neovarsity.toursattractions.ingestion.model.CanonicalPaxType;
import com.neovarsity.toursattractions.ingestion.model.CanonicalProduct;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
public class ViatorVendorMapper implements VendorProductMapper {

    private final ObjectMapper objectMapper;

    public ViatorVendorMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String vendorCode() {
        return "VIATOR";
    }

    @Override
    public List<CanonicalProduct> map(String rawPayload) {
        try {
            JsonNode root = objectMapper.readTree(rawPayload);
            List<CanonicalProduct> products = new ArrayList<>();
            for (JsonNode node : root.path("products")) {
                JsonNode pricing = node.path("pricing");
                List<CanonicalActivity> activities = new ArrayList<>();
                for (JsonNode act : node.path("activities")) {
                    activities.add(CanonicalActivity.builder()
                            .externalCode(act.path("activityId").asText())
                            .title(act.path("name").asText())
                            .description(act.path("details").asText(""))
                            .build());
                }
                List<CanonicalPaxType> pax = List.of(
                        pax("ADULT", "Adult", 12, 99, pricing.path("adult"), pricing.path("currency").asText("INR")),
                        pax("CHILD", "Child", 3, 11, pricing.path("child"), pricing.path("currency").asText("INR")),
                        pax("INFANT", "Infant", 0, 2, pricing.path("infant"), pricing.path("currency").asText("INR"))
                );
                products.add(CanonicalProduct.builder()
                        .externalId(node.path("productCode").asText())
                        .title(node.path("productName").asText())
                        .description(node.path("summary").asText(""))
                        .cityCode(node.path("destinationCode").asText())
                        .categoryName(node.path("category").asText("Culture"))
                        .type(parseType(node.path("productType").asText("TOUR")))
                        .basePrice(BigDecimal.valueOf(pricing.path("adult").asDouble(0)))
                        .currency(pricing.path("currency").asText("INR"))
                        .durationHours(node.path("durationHours").asInt(3))
                        .imageUrl(node.path("heroImage").asText(null))
                        .rating(BigDecimal.valueOf(node.path("rating").asDouble(4.5)))
                        .reviewCount(node.path("reviewCount").asInt(0))
                        .activities(activities)
                        .paxTypes(pax)
                        .build());
            }
            return products;
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid Viator payload: " + e.getMessage(), e);
        }
    }

    private static CanonicalPaxType pax(String code, String label, int min, int max, JsonNode priceNode, String currency) {
        return CanonicalPaxType.builder()
                .code(code)
                .label(label)
                .minAge(min)
                .maxAge(max)
                .price(BigDecimal.valueOf(priceNode.asDouble(0)))
                .currency(currency)
                .build();
    }

    private static AttractionType parseType(String raw) {
        try {
            return AttractionType.valueOf(raw.toUpperCase());
        } catch (Exception e) {
            return AttractionType.TOUR;
        }
    }
}
