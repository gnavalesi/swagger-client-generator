package com.navent.swagger.client.implementation.config;

import org.springframework.http.client.HttpComponentsAsyncClientHttpRequestFactory;
import org.zalando.riptide.Http;

public abstract class RestClientConfiguration {
    private final Http executor;

    public RestClientConfiguration(String baseUrl) {
        this.executor = buildExecutor(baseUrl);
    }


    private Http buildExecutor(String baseUrl) {
        return Http.builder()
                .baseUrl(baseUrl)
                .requestFactory(getHttpComponentsAsyncClientHttpRequestFactory())
                .build();
    }

    protected HttpComponentsAsyncClientHttpRequestFactory getHttpComponentsAsyncClientHttpRequestFactory() {
        return new HttpComponentsAsyncClientHttpRequestFactory();
    }
}
