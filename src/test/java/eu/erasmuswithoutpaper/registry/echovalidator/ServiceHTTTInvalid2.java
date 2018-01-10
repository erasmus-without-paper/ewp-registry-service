package eu.erasmuswithoutpaper.registry.echovalidator;

import eu.erasmuswithoutpaper.registry.internet.Request;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

/**
 * This endpoint is invalid, because it requires the Date header to be present and signed. Whereas
 * the specs says that Date *OR* Original-Date must be signed.
 */
public class ServiceHTTTInvalid2 extends ServiceHTTTValid {

  public ServiceHTTTInvalid2(String url, RegistryClient registryClient) {
    super(url, registryClient);
  }

  @Override
  protected void verifyDateAndOriginalDateHeaders(Request request) throws ErrorResponseException {
    if (request.getHeader("Date") == null) {
      throw new ErrorResponseException(this.createErrorResponse(request, 400,
          "This endpoint requires your request to include the \"Date\" header."));
    }
    super.verifyDateAndOriginalDateHeaders(request);
  }
}
