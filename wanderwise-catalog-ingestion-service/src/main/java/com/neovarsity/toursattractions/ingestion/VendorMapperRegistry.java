package com.neovarsity.toursattractions.ingestion;

import com.neovarsity.toursattractions.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class VendorMapperRegistry {

    private final Map<String, VendorProductMapper> byCode;

    public VendorMapperRegistry(List<VendorProductMapper> mappers) {
        this.byCode = mappers.stream()
                .collect(Collectors.toMap(m -> m.vendorCode().toUpperCase(), Function.identity()));
    }

    public VendorProductMapper getRequired(String vendorCode) {
        VendorProductMapper mapper = byCode.get(vendorCode.toUpperCase());
        if (mapper == null) {
            throw new BusinessException("No ingestion mapper registered for vendor: " + vendorCode);
        }
        return mapper;
    }
}
