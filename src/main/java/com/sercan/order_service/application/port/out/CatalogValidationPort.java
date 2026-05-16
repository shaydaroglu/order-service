package com.sercan.order_service.application.port.out;

import java.util.List;
import java.util.UUID;

public interface CatalogValidationPort {
    void validateProductOfferings(List<UUID> productOfferingIds);
}
