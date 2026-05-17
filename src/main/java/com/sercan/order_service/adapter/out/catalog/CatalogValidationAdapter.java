package com.sercan.order_service.adapter.out.catalog;

import com.sercan.order_service.application.port.out.CatalogValidationPort;
import com.sercan.order_service.domain.exception.CatalogServiceException;
import com.sercan.order_service.domain.exception.InvalidProductOfferingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import retrofit2.Response;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class CatalogValidationAdapter implements CatalogValidationPort {
    private final CatalogClient catalogClient;

    @Override
    public void validateProductOfferings(List<UUID> productOfferingIds) {
        log.debug("Calling catalog service to validate {} product offerings", productOfferingIds.size());
        try {
            Response<Void> response = catalogClient
                    .validateProductOfferings(productOfferingIds)
                    .execute();

            if (response.isSuccessful()) {
                log.debug("All product offerings validated successfully");
                return;
            }

            if (response.code() == 404) {
                String errorBody = response.errorBody() != null
                        ? response.errorBody().string()
                        : "Unknown product offerings";
                log.warn("Product offerings not found: {}", errorBody);
                throw new InvalidProductOfferingException("Invalid product offerings: " + errorBody);
            }

            log.error("Catalog service returned unexpected status: {}", response.code());
            throw new CatalogServiceException("Catalog service returned unexpected status: " + response.code());

        } catch (IOException e) {
            log.error("Catalog service is unavailable: {}", e.getMessage());
            throw new CatalogServiceException("Catalog service is unavailable");
        }
    }
}
