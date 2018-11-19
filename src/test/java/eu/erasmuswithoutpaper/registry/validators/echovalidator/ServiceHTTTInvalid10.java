package eu.erasmuswithoutpaper.registry.validators.echovalidator;

import eu.erasmuswithoutpaper.registry.internet.Request;
import eu.erasmuswithoutpaper.registry.internet.sec.EwpHttpSigRequestAuthorizer;
import eu.erasmuswithoutpaper.registry.internet.sec.Http4xx;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

/**
 * Invalid, because it doesn't verify the digest.
 */
public class ServiceHTTTInvalid10 extends ServiceHTTTValid {

  public ServiceHTTTInvalid10(String url, RegistryClient registryClient) {
    super(url, registryClient);
  }

  @Override
  protected EwpHttpSigRequestAuthorizer newAuthorizer() {
    return new EwpHttpSigRequestAuthorizer(this.registryClient) {
      @Override
      protected void verifyDigestHeader(Request request) throws Http4xx {
        return;
      }
    };
  }

}
