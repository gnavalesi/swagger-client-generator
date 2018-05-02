package com.navent.swagger.client.implementation.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsAsyncClientHttpRequestFactory;
import org.zalando.riptide.Http;

@Configuration
public abstract class RestClientConfiguration {
	private final Http http;
	private final long futureTimeout;

	@Autowired
	public RestClientConfiguration(RestClientConfig config) {
		this.http = buildExecutor(config);
		this.futureTimeout = config.getFutureTimeout();
	}

	public final Http getHttp() {
		return this.http;
	}

	public final long getFutureTimeout() {
		return this.futureTimeout;
	}

	private Http buildExecutor(RestClientConfig config) {
		return Http.builder()
				.baseUrl(config.getBasePath())
				.requestFactory(getHttpComponentsAsyncClientHttpRequestFactory())
				.build();
	}

	protected HttpComponentsAsyncClientHttpRequestFactory getHttpComponentsAsyncClientHttpRequestFactory() {
		return new HttpComponentsAsyncClientHttpRequestFactory();
	}
}
