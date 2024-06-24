package io.quarkus.ts.http.restclient.reactive;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.reactive.server.multipart.MultipartFormDataInput;

@Path("/simple")
@RegisterRestClient(configKey = "simple-encode-api")
public interface RestEncodeClient {

    @POST
    @Path("/encode")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    MyMultipartDTO doAPostRequestToThisResource(MultipartFormDataInput multipartFormDataInput);
}
