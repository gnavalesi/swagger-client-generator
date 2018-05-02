package com.navent.swagger.client.implementation.config;

import org.springframework.context.annotation.Configuration;
import org.zalando.riptide.Http;
import org.zalando.riptide.spring.ClientHttpMessageConverters;

@Configuration
public abstract class RestClientConfiguration {
	public abstract Http http();

	public abstract long getFutureTimeout();

	public abstract ClientHttpMessageConverters getClientHttpMessageConverters();
}
