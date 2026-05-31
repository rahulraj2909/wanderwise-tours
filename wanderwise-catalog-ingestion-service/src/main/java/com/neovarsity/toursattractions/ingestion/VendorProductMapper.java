package com.neovarsity.toursattractions.ingestion;

import com.neovarsity.toursattractions.ingestion.model.CanonicalProduct;

import java.util.List;

/**
 * Maps a vendor-specific payload into WanderWise canonical catalog models.
 */
public interface VendorProductMapper {

    String vendorCode();

    List<CanonicalProduct> map(String rawPayload);
}
