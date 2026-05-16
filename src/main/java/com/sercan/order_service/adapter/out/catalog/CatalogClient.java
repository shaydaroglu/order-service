package com.sercan.order_service.adapter.out.catalog;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

import java.util.List;
import java.util.UUID;

public interface CatalogClient {

    @POST("api/v1/products/validate")
    Call<Void> validateProductOfferings(@Body List<UUID> productOfferingIds);
}
