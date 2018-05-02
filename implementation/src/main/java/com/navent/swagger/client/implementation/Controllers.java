package com.navent.swagger.client.implementation;

import com.navent.swagger.client.implementation.exceptions.InformationalStatusException;
import com.navent.swagger.client.implementation.exceptions.RedirectionStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.zalando.riptide.*;

import java.util.List;
import java.util.concurrent.*;

public class Controllers {
	private Controllers() {
	}

	private static Binding<HttpStatus.Series> informationalBinding(CompletableFuture<?> result) {
		return Bindings.on(HttpStatus.Series.INFORMATIONAL).call(r ->
				result.completeExceptionally(new InformationalStatusException(r.getStatusCode()))
		);
	}

	private static Binding<HttpStatus.Series> redirectionBinding(CompletableFuture<?> result) {
		return Bindings.on(HttpStatus.Series.REDIRECTION).call(r ->
				result.completeExceptionally(new RedirectionStatusException(r.getStatusCode()))
		);
	}

	private static Binding<HttpStatus.Series> serverErrorBinding(CompletableFuture<?> result) {
		return Bindings.on(HttpStatus.Series.SERVER_ERROR).call(r ->
				result.completeExceptionally(new HttpServerErrorException(r.getStatusCode()))
		);
	}

	private static Binding<HttpStatus.Series> clientErrorBinding(CompletableFuture<?> result) {
		return Bindings.on(HttpStatus.Series.CLIENT_ERROR).call(r ->
				result.completeExceptionally(new HttpClientErrorException(r.getStatusCode()))
		);
	}

	private static <T> Binding<HttpStatus.Series> successfulBinding(CompletableFuture<T> result, Class<T> responseClass) {
		return Bindings.on(HttpStatus.Series.SUCCESSFUL).call(Types.responseEntityOf(responseClass), r ->
				result.complete(r.getBody())
		);
	}

	private static <T> Binding<HttpStatus.Series> successfulListBinding(CompletableFuture<List<T>> result, Class<T> responseClass) {
		return Bindings.on(HttpStatus.Series.SUCCESSFUL).call(Types.listOf(responseClass), result::complete);
	}

	public static <T> T resolveFuture(Future<T> future, long timeout, TimeUnit timeUnit) throws InterruptedException, ExecutionException, TimeoutException {
		return future.get(timeout, timeUnit);
	}

	public static <T> Future<T> dispatch(Class<T> responseClass, Dispatcher dispatcher) {
		CompletableFuture<T> result = new CompletableFuture<>();
		dispatcher.dispatch(Navigators.series(),
				informationalBinding(result),
				successfulBinding(result, responseClass),
				redirectionBinding(result),
				clientErrorBinding(result),
				serverErrorBinding(result));
		return result;
	}

	public static <T> Future<List<T>> dispatchList(Class<T> responseClass, Dispatcher dispatcher) {
		CompletableFuture<List<T>> result = new CompletableFuture<>();
		dispatcher.dispatch(Navigators.series(),
				informationalBinding(result),
				successfulListBinding(result, responseClass),
				redirectionBinding(result),
				clientErrorBinding(result),
				serverErrorBinding(result));
		return result;
	}

	public static <T> String toQueryParameter(T object, Class<? extends T> theClass) {
		return object.toString();
	}

	public static <T> String toHeaderParameter(T object, Class<? extends T> theClass) {
		return object.toString();
	}
}
