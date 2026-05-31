package com.neovarsity.toursattractions.util;

import org.springframework.data.domain.Sort;

/**
 * Maps public sort keys (listing page) to JPA {@link Sort} — popularity/ranking, price, rating, etc.
 */
public final class AttractionSortBuilder {

    private AttractionSortBuilder() {
    }

    public static Sort build(String sortBy, String direction) {
        String key = sortBy == null ? "popularity" : sortBy.toLowerCase().replace("-", "_");
        boolean desc = !"asc".equalsIgnoreCase(direction);

        return switch (key) {
            case "popularity", "ranking", "recommended" -> Sort.by(Sort.Direction.DESC, "averageRating")
                    .and(Sort.by(Sort.Direction.DESC, "reviewCount"));
            case "price_asc", "price_low" -> Sort.by(Sort.Direction.ASC, "price");
            case "price_desc", "price_high" -> Sort.by(Sort.Direction.DESC, "price");
            case "rating" -> Sort.by(Sort.Direction.DESC, "averageRating");
            case "reviews", "most_reviewed" -> Sort.by(Sort.Direction.DESC, "reviewCount");
            case "duration" -> desc
                    ? Sort.by(Sort.Direction.DESC, "durationHours")
                    : Sort.by(Sort.Direction.ASC, "durationHours");
            case "price" -> desc ? Sort.by(Sort.Direction.DESC, "price") : Sort.by(Sort.Direction.ASC, "price");
            default -> Sort.by(Sort.Direction.DESC, "averageRating")
                    .and(Sort.by(Sort.Direction.DESC, "reviewCount"));
        };
    }
}
