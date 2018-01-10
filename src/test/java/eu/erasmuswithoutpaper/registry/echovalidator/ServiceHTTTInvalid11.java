package eu.erasmuswithoutpaper.registry.echovalidator;

import eu.erasmuswithoutpaper.registry.internet.Request;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

/**
 * A bit invalid, because it allows non-UUID X-Request-Id.
 */
public class ServiceHTTTInvalid11 extends ServiceHTTTValid {

  public ServiceHTTTInvalid11(String url, RegistryClient registryClient) {
    super(url, registryClient);
  }

  @Override
  protected void verifyRequestIdHeader(Request request) throws ErrorResponseException {
    String value = request.getHeader("X-Request-Id");
    if (value == null) {
      throw new ErrorResponseException(
          this.createErrorResponse(request, 400, "Missing \"X-Request-Id\" header"));
    }
  }
}
