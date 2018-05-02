package com.navent.swagger.client.implementation;

import com.navent.swagger.client.implementation.config.RestClientConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.zalando.riptide.Http;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
public abstract class Controller {

	private final RestClientConfiguration config;

	@Autowired
	public Controller(RestClientConfiguration config) {
		this.config = config;
	}

	protected Http http() {
		return config.http();
	}

	protected <T> T resolveFuture(Future<T> future) throws InterruptedException, ExecutionException, TimeoutException {
		return Controllers.resolveFuture(future, config.getFutureTimeout(), TimeUnit.SECONDS);
	}
}
