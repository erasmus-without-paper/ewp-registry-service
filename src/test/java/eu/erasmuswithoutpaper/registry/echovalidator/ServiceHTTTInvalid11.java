package eu.erasmuswithoutpaper.registry.echovalidator;

import eu.erasmuswithoutpaper.registry.internet.Request;
import eu.erasmuswithoutpaper.registry.internet.sec.EwpHttpSigRequestAuthorizer;
import eu.erasmuswithoutpaper.registry.internet.sec.Http4xx;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

/**
 * A bit invalid, because it allows non-UUID X-Request-Id.
 */
public class ServiceHTTTInvalid11 extends ServiceHTTTValid {

  public ServiceHTTTInvalid11(String url, RegistryClient registryClient) {
    super(url, registryClient);
  }

  @Override
  protected EwpHttpSigRequestAuthorizer newAuthorizer() {
    return new EwpHttpSigRequestAuthorizer(this.registryClient) {
      @Override
      protected void verifyRequestIdHeader(Request request) throws Http4xx {
        String value = request.getHeader("X-Request-Id");
        if (value == null) {
          throw new Http4xx(400, "Missing \"X-Request-Id\" header");
        }
      }
    };
  }
}
