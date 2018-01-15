package eu.erasmuswithoutpaper.registry.echovalidator;

import eu.erasmuswithoutpaper.registry.internet.Request;
import eu.erasmuswithoutpaper.registry.internet.sec.EwpHttpSigRequestAuthorizer;
import eu.erasmuswithoutpaper.registry.internet.sec.Http4xx;
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
  protected EwpHttpSigRequestAuthorizer newAuthorizer() {
    return new EwpHttpSigRequestAuthorizer(this.registryClient) {
      @Override
      protected void verifyDateAndOriginalDateHeaders(Request request) throws Http4xx {
        if (request.getHeader("Date") == null) {
          throw new Http4xx(400,
              "This endpoint requires your request to include the \"Date\" header.");
        }
        super.verifyDateAndOriginalDateHeaders(request);
      }
    };
  }
}
