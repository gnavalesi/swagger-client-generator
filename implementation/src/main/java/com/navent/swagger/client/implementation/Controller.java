package com.navent.swagger.client.implementation;

import com.navent.swagger.client.implementation.config.RestClientConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.stereotype.Component;
import org.zalando.riptide.Http;
import org.zalando.riptide.spring.ClientHttpMessageConverters;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
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

	protected <T> String toQueryParameter(T object, Class<? extends T> theClass) {
		return config.getClientHttpMessageConverters().getConverters().stream()
				.filter(s -> s.canWrite(theClass, MediaType.APPLICATION_JSON))
				.findFirst()
				.map(c -> {
					try {
						return SimpleHttpOutputMessage.from((HttpMessageConverter<T>) c, object);
					} catch (IOException e) {
						return null;
					}
				})
				.filter(Objects::nonNull)
				.orElseGet(object::toString);
	}

	public <T> String toHeaderParameter(T object, Class<? extends T> theClass) {
		return config.getClientHttpMessageConverters().getConverters().stream()
				.filter(s -> s.canWrite(theClass, MediaType.APPLICATION_JSON))
				.findFirst()
				.map(c -> {
					try {
						return SimpleHttpOutputMessage.from((HttpMessageConverter<T>) c, object);
					} catch (IOException e) {
						return null;
					}
				})
				.filter(Objects::nonNull)
				.orElseGet(object::toString);
	}

	static class SimpleHttpOutputMessage implements HttpOutputMessage {
		private OutputStream os = new ByteArrayOutputStream();
		private HttpHeaders headers = new HttpHeaders();

		static <T> String from(HttpMessageConverter<T> converter, T object) throws IOException {
			SimpleHttpOutputMessage res = new SimpleHttpOutputMessage();
			converter.write(object, MediaType.APPLICATION_JSON, res);
			return res.getBody().toString();
		}

		@Override
		public OutputStream getBody() throws IOException {
			return os;
		}

		@Override
		public HttpHeaders getHeaders() {
			return headers;
		}
	}
}
