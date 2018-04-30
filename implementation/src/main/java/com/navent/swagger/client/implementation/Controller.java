package com.navent.swagger.client.implementation;

import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.zalando.riptide.Http;
import org.zalando.riptide.HttpBuilder;
import org.zalando.riptide.Requester;

import java.net.URI;

import static org.zalando.riptide.Navigators.series;

@Component
public abstract class Controller {

    private Http http;

    public Requester get(String uriTemplate, Object... urlVariables) {
        return http.get(uriTemplate, urlVariables);
    }

    public Requester get(URI uri) {
        return http.get(uri);
    }

    public Requester get() {
        return http.get();
    }

    public Requester head(String uriTemplate, Object... urlVariables) {
        return http.head(uriTemplate, urlVariables);
    }

    public Requester head(URI uri) {
        return http.head(uri);
    }

    public Requester head() {
        return http.head();
    }

    public Requester post(String uriTemplate, Object... urlVariables) {
        return http.post(uriTemplate, urlVariables);
    }

    public Requester post(URI uri) {
        return http.post(uri);
    }

    public Requester post() {
        return http.post();
    }

    public Requester put(String uriTemplate, Object... urlVariables) {
        return http.put(uriTemplate, urlVariables);
    }

    public Requester put(URI uri) {
        return http.put(uri);
    }

    public Requester put() {
        return http.put();
    }

    public Requester patch(String uriTemplate, Object... urlVariables) {
        return http.patch(uriTemplate, urlVariables);
    }

    public Requester patch(URI uri) {
        return http.patch(uri);
    }

    public Requester patch() {
        return http.patch();
    }

    public Requester delete(String uriTemplate, Object... urlVariables) {
        return http.delete(uriTemplate, urlVariables);
    }

    public Requester delete(URI uri) {
        return http.delete(uri);
    }

    public Requester delete() {
        return http.delete();
    }

    public Requester options(String uriTemplate, Object... urlVariables) {
        return http.options(uriTemplate, urlVariables);
    }

    public Requester options(URI uri) {
        return http.options(uri);
    }

    public Requester options() {
        return http.options();
    }

    public Requester trace(String uriTemplate, Object... urlVariables) {
        return http.trace(uriTemplate, urlVariables);
    }

    public Requester trace(URI uri) {
        return http.trace(uri);
    }

    public Requester trace() {
        return http.trace();
    }

    public Requester execute(HttpMethod method, String uriTemplate, Object... uriVariables) {
        return http.execute(method, uriTemplate, uriVariables);
    }

    public Requester execute(HttpMethod method, URI uri) {
        return http.execute(method, uri);
    }

    public Requester execute(HttpMethod method) {
        return http.execute(method);
    }

    public static HttpBuilder builder() {
        return Http.builder();
    }


}
