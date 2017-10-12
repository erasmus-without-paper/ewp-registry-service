package eu.erasmuswithoutpaper.registry.echovalidator;

import java.io.IOException;

import eu.erasmuswithoutpaper.registry.internet.Internet.Request;
import eu.erasmuswithoutpaper.registry.internet.Internet.Response;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

/**
 * Internal "fake" implementation of a valid TTTT API endpoint.
 */
public class ServiceTTTTDummy extends AbstractEchoV2Service {

  public ServiceTTTTDummy(String url, RegistryClient registryClient) {
    super(url, registryClient);
  }

  @Override
  public Response handleInternetRequest2(Request request) throws IOException {
    if (!request.getUrl().startsWith(this.myEndpoint)) {
      return null;
    }
    // Since the first "T" is not implemented by the validator, most of the tests will be skipped.
    return this.createErrorResponse(request, 404, "Not implemented.");
  }

}
