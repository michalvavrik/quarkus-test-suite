package io.quarkus.ts.http.restclient.reactive.resources;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.server.multipart.MultipartFormDataInput;

import io.quarkus.ts.http.restclient.reactive.MyMultipartDTO;
import io.quarkus.ts.http.restclient.reactive.RestEncodeClient;

@Path("/encode")
public class SimpleEncodeResource {

    @RestClient
    RestEncodeClient restEncodeClient;

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public MyMultipartDTO doAPostRequestToSimpleEncodeResource(MultipartFormDataInput multipartFormDataInput) {
        return restEncodeClient.doAPostRequestToThisResource(multipartFormDataInput);
    }

}
