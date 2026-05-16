package com.sercan.order_service.adapter.out.catalog;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

import java.util.concurrent.TimeUnit;

@Configuration
public class CatalogClientConfig {
    @Value("${catalog.service.url}")
    private String catalogServiceUrl;

    @Value("${catalog.service.timeout-seconds:5}")
    private int timeoutSeconds;

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Bean
    public CatalogClient catalogClient(ObjectMapper objectMapper) {
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .writeTimeout(timeoutSeconds, TimeUnit.SECONDS)
                .build();

        return new Retrofit.Builder()
                .baseUrl(catalogServiceUrl)
                .client(okHttpClient)
                .addConverterFactory(JacksonConverterFactory.create(objectMapper))
                .build()
                .create(CatalogClient.class);
    }
}
