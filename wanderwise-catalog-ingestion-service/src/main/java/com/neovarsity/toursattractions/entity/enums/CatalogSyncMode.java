package com.neovarsity.toursattractions.entity.enums;

/**
 * INGEST_ONLY — create new catalog rows only (skip existing vendor products).
 * UPDATE_ONLY — refresh existing rows only (skip new products in feed).
 * FULL — create and update (manual pull).
 */
public enum CatalogSyncMode {
    INGEST_ONLY,
    UPDATE_ONLY,
    FULL
}
