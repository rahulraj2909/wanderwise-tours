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

/**
 * Maps GetYourGuide Kafka catalog events (single product per message).
 */
@Component
public class GetYourGuideVendorMapper implements VendorProductMapper {

    private final ObjectMapper objectMapper;

    public GetYourGuideVendorMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String vendorCode() {
        return "GETYOURGUIDE";
    }

    @Override
    public List<CanonicalProduct> map(String rawPayload) {
        try {
            JsonNode root = objectMapper.readTree(rawPayload);
            JsonNode payload = root.has("payload") ? root.path("payload") : root;
            List<CanonicalActivity> activities = new ArrayList<>();
            for (JsonNode opt : payload.path("options")) {
                activities.add(CanonicalActivity.builder()
                        .externalCode(opt.path("optionId").asText())
                        .title(opt.path("name").asText())
                        .description(opt.path("description").asText(""))
                        .build());
            }
            List<CanonicalPaxType> paxTypes = new ArrayList<>();
            for (JsonNode tt : payload.path("travelerTypes")) {
                JsonNode price = tt.path("price");
                paxTypes.add(CanonicalPaxType.builder()
                        .code(tt.path("code").asText())
                        .label(tt.path("label").asText())
                        .minAge(tt.path("fromAge").asInt(0))
                        .maxAge(tt.path("toAge").asInt(99))
                        .price(BigDecimal.valueOf(price.path("amount").asDouble(0)))
                        .currency(price.path("currency").asText("INR"))
                        .build());
            }
            BigDecimal base = paxTypes.stream()
                    .filter(p -> "ADULT".equals(p.code()))
                    .map(CanonicalPaxType::price)
                    .findFirst()
                    .orElse(BigDecimal.ZERO);
            return List.of(CanonicalProduct.builder()
                    .externalId(payload.path("id").asText())
                    .title(payload.path("title").asText())
                    .description(payload.path("shortDescription").asText(""))
                    .cityCode(payload.path("location").path("cityCode").asText())
                    .categoryName("Culture")
                    .type(AttractionType.TOUR)
                    .basePrice(base)
                    .currency("INR")
                    .durationHours(payload.path("duration").path("hours").asInt(3))
                    .imageUrl(payload.path("media").path("primaryImage").asText(null))
                    .rating(BigDecimal.valueOf(payload.path("rating").path("average").asDouble(4.5)))
                    .reviewCount(payload.path("rating").path("count").asInt(0))
                    .activities(activities)
                    .paxTypes(paxTypes)
                    .build());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid GetYourGuide payload: " + e.getMessage(), e);
        }
    }
}
