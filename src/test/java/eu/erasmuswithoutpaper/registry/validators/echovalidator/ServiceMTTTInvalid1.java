package eu.erasmuswithoutpaper.registry.validators.echovalidator;

import java.io.IOException;
import java.util.Collections;

import eu.erasmuswithoutpaper.registry.internet.Request;
import eu.erasmuswithoutpaper.registry.internet.Response;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

/**
 * Invalid, because it allows anonymous access.
 */
public class ServiceMTTTInvalid1 extends ServiceMTTTValid {

  public ServiceMTTTInvalid1(String url, RegistryClient registryClient) {
    super(url, registryClient);
  }

  @Override
  public Response handleInternetRequest2(Request request)
      throws IOException, ErrorResponseException {
    if (!request.getUrl().startsWith(this.myEndpoint)) {
      return null;
    }
    if (request.getHeader("Authorization") != null) {
      return this.httt.handleInternetRequest2(request);
    } else {
      return this.createEchoResponse(request, this.retrieveEchoValues(request),
          Collections.emptyList());
    }
  }
}
