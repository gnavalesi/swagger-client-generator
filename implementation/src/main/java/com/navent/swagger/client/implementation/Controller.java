package com.navent.swagger.client.implementation;

import com.navent.swagger.client.implementation.exceptions.InformationalStatusException;
import com.navent.swagger.client.implementation.exceptions.RedirectionStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.zalando.riptide.*;

import java.util.List;
import java.util.concurrent.*;

@Component
public abstract class Controller {

	protected Http http;
	protected long timeout = 60;

	protected static Binding<HttpStatus.Series> informationalBinding(CompletableFuture<?> result) {
		return Bindings.on(HttpStatus.Series.INFORMATIONAL).call(r ->
				result.completeExceptionally(new InformationalStatusException(r.getStatusCode()))
		);
	}

	protected static Binding<HttpStatus.Series> redirectionBinding(CompletableFuture<?> result) {
		return Bindings.on(HttpStatus.Series.REDIRECTION).call(r ->
				result.completeExceptionally(new RedirectionStatusException(r.getStatusCode()))
		);
	}

	protected static Binding<HttpStatus.Series> serverErrorBinding(CompletableFuture<?> result) {
		return Bindings.on(HttpStatus.Series.SERVER_ERROR).call(r ->
				result.completeExceptionally(new HttpServerErrorException(r.getStatusCode()))
		);
	}

	protected static Binding<HttpStatus.Series> clientErrorBinding(CompletableFuture<?> result) {
		return Bindings.on(HttpStatus.Series.CLIENT_ERROR).call(r ->
				result.completeExceptionally(new HttpClientErrorException(r.getStatusCode()))
		);
	}

	protected static <T> Binding<HttpStatus.Series> successfulBinding(CompletableFuture<T> result, Class<T> responseClass) {
		return Bindings.on(HttpStatus.Series.SUCCESSFUL).call(Types.responseEntityOf(responseClass), r ->
				result.complete(r.getBody())
		);
	}

	protected static <T> Binding<HttpStatus.Series> successfulListBinding(CompletableFuture<List<T>> result, Class<T> responseClass) {
		return Bindings.on(HttpStatus.Series.SUCCESSFUL).call(Types.listOf(responseClass), result::complete);
	}

	protected <T> T resolveFuture(Future<T> future) throws InterruptedException, ExecutionException, TimeoutException {
		return future.get(timeout, TimeUnit.SECONDS);
	}

	protected <T> Future<T> dispatch(Class<T> responseClass, Dispatcher dispatcher) {
		CompletableFuture<T> result = new CompletableFuture<>();
		dispatcher.dispatch(Navigators.series(),
				informationalBinding(result),
				successfulBinding(result, responseClass),
				redirectionBinding(result),
				clientErrorBinding(result),
				serverErrorBinding(result));
		return result;
	}

	protected <T> Future<List<T>> dispatchList(Class<T> responseClass, Dispatcher dispatcher) {
		CompletableFuture<List<T>> result = new CompletableFuture<>();
		dispatcher.dispatch(Navigators.series(),
				informationalBinding(result),
				successfulListBinding(result, responseClass),
				redirectionBinding(result),
				clientErrorBinding(result),
				serverErrorBinding(result));
		return result;
	}

	protected <T> String toQueryParameter(T object, Class<? extends T> theClass) {
		return object.toString();
	}

	protected <T> String toHeaderParameter(T object, Class<? extends T> theClass) {
		return object.toString();
	}
}
