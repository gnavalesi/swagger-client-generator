package com.navent.swagger.client.implementation.config;

import org.springframework.context.annotation.Configuration;
import org.zalando.riptide.Http;

@Configuration
public abstract class RestClientConfiguration {
	public abstract Http http();

	public abstract long getFutureTimeout();
}
