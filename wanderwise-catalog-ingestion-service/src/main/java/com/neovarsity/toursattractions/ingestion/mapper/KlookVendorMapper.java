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
public class KlookVendorMapper implements VendorProductMapper {

    private final ObjectMapper objectMapper;

    public KlookVendorMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String vendorCode() {
        return "KLOOK";
    }

    @Override
    public List<CanonicalProduct> map(String rawPayload) {
        try {
            JsonNode root = objectMapper.readTree(rawPayload);
            List<CanonicalProduct> products = new ArrayList<>();
            for (JsonNode sku : root.path("sku_list")) {
                List<CanonicalPaxType> paxTypes = new ArrayList<>();
                for (JsonNode pax : sku.path("pax_types")) {
                    paxTypes.add(CanonicalPaxType.builder()
                            .code(pax.path("type").asText())
                            .label(pax.path("display").asText())
                            .minAge(pax.path("min_age").asInt(0))
                            .maxAge(pax.path("max_age").asInt(99))
                            .price(BigDecimal.valueOf(pax.path("sell_price").asDouble(0)))
                            .currency("INR")
                            .build());
                }
                BigDecimal base = paxTypes.stream()
                        .filter(p -> "ADULT".equals(p.code()))
                        .map(CanonicalPaxType::price)
                        .findFirst()
                        .orElse(BigDecimal.ZERO);
                products.add(CanonicalProduct.builder()
                        .externalId(sku.path("sku_id").asText())
                        .title(sku.path("name").asText())
                        .description(sku.path("description").asText(""))
                        .cityCode(sku.path("city_code").asText())
                        .categoryName(sku.path("category_name").asText("Adventure"))
                        .type(parseType(sku.path("sku_type").asText("TOUR")))
                        .basePrice(base)
                        .currency("INR")
                        .durationHours(sku.path("duration_h").asInt(3))
                        .imageUrl(sku.path("cover_url").asText(null))
                        .rating(BigDecimal.valueOf(sku.path("score").asDouble(4.5)))
                        .reviewCount(sku.path("reviews").asInt(0))
                        .activities(List.of(CanonicalActivity.builder()
                                .externalCode("DEFAULT")
                                .title("Standard experience")
                                .description("Default Klook activity option")
                                .build()))
                        .paxTypes(paxTypes)
                        .build());
            }
            return products;
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid Klook payload: " + e.getMessage(), e);
        }
    }

    private static AttractionType parseType(String raw) {
        try {
            return AttractionType.valueOf(raw.toUpperCase());
        } catch (Exception e) {
            return AttractionType.TOUR;
        }
    }
}
