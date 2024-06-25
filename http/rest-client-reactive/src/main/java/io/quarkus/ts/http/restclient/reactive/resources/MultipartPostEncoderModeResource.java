package io.quarkus.ts.http.restclient.reactive.resources;

import java.util.Map;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.server.multipart.MultipartFormDataInput;

import io.quarkus.ts.http.restclient.reactive.EncoderModeRestClient;
import io.quarkus.ts.http.restclient.reactive.Html5EncoderModeRestClient;
import io.quarkus.ts.http.restclient.reactive.MyMultipartDTO;
import io.quarkus.ts.http.restclient.reactive.Rfc1738EncoderModeRestClient;
import io.quarkus.ts.http.restclient.reactive.Rfc3986EncoderModeRestClient;

@Path("encode")
public class MultipartPostEncoderModeResource {

    private final Map<String, EncoderModeRestClient> modeToRestClient;

    public MultipartPostEncoderModeResource(@RestClient Html5EncoderModeRestClient html5Client,
            @RestClient Rfc1738EncoderModeRestClient rfc1738Client,
            @RestClient Rfc3986EncoderModeRestClient rfc3986Client) {
        this.modeToRestClient = Map.of("HTML5", html5Client, "RFC1738", rfc1738Client, "RFC3986", rfc3986Client);
    }

    @Path("{encoder-mode}")
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public MyMultipartDTO doAPostRequestToSimpleEncodeResource(MultipartFormDataInput multipartFormDataInput,
            @PathParam("encoder-mode") String encoderMode) {
        System.out.println("////// multipart data are " + multipartFormDataInput.getValues()); // FIXME: remove me!
        // FIXME: this needs heavy refactoring
        var file = multipartFormDataInput.getValues().get("file").stream().findFirst().get().getFileItem().getFile();
        System.out.println("file is " + file); // FIXME: remove me
        var otherField = multipartFormDataInput.getValues().get("otherField").stream().findFirst().get().getValue();
        return modeToRestClient.get(encoderMode).doAPostRequestToThisResource(file, file, otherField);
    }

}
