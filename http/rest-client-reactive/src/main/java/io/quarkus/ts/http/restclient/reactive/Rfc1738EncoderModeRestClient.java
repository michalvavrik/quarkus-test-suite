package io.quarkus.ts.http.restclient.reactive;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "encoder-mode-rfc1738")
public interface Rfc1738EncoderModeRestClient extends EncoderModeRestClient {

}
