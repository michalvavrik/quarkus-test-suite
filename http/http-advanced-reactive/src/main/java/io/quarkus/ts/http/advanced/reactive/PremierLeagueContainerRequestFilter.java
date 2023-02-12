package io.quarkus.ts.http.advanced.reactive;

import java.util.Objects;

import jakarta.enterprise.inject.Instance;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.Provider;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.vertx.core.http.HttpServerRequest;

@Provider
public class PremierLeagueContainerRequestFilter implements ContainerRequestFilter {

    public static final String REQ_PARAM_NAME = "footballer-name";

    @ConfigProperty(name = "pl-container-request-filter.enabled")
    Instance<Boolean> filterEnabled;

    @Context
    HttpServerRequest httpRequest;

    @Context
    UriInfo uriInfo;

    @Override
    public void filter(ContainerRequestContext requestContext) {

        if (filterEnabled.stream().findAny().orElse(Boolean.FALSE)) {

            var nameFromHttpRequest = httpRequest.getParam(REQ_PARAM_NAME);
            var nameFromUriInfo = uriInfo.getQueryParameters().getFirst(REQ_PARAM_NAME);
            if (!Objects.equals(nameFromHttpRequest, nameFromUriInfo)) {
                throw new IllegalStateException("Values from uriInfo and httpRequest differ");
            }
            if (nameFromHttpRequest == null) {
                requestContext.abortWith(Response.status(Status.CONFLICT).build());
            }
        }
    }
}
