package io.quarkus.ts.http.restclient.reactive.resources;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.server.multipart.FormValue;
import org.jboss.resteasy.reactive.server.multipart.MultipartFormDataInput;

import io.quarkus.ts.http.restclient.reactive.Item;

@Path("/simple")
public class RestEncodeClientResource {

    @POST
    @Path("/encode")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public List<Item> doAPostRequestToRestEncodeClientResource(MultipartFormDataInput multipartFormDataInput)
            throws IOException {
        Map<String, Collection<FormValue>> map = multipartFormDataInput.getValues();
        List<Item> items = new ArrayList<>();
        for (var entry : map.entrySet()) {
            for (FormValue value : entry.getValue()) {
                items.add(new Item(
                        entry.getKey(),
                        value.isFileItem() ? value.getFileItem().getFileSize() : value.getValue().length(),
                        value.getCharset(),
                        value.getFileName(),
                        value.isFileItem(),
                        value.getHeaders()));
            }

        }
        return items;
    }
}
