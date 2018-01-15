package eu.erasmuswithoutpaper.registry.echovalidator;

import eu.erasmuswithoutpaper.registry.internet.sec.EwpHttpSigRequestAuthorizer;
import eu.erasmuswithoutpaper.registry.internet.sec.Http4xx;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

/**
 * This one is a bit invalid, because it doesn't include the Want-Digest header in HTTP 401
 * responses.
 */
public class ServiceHTTTInvalid5 extends ServiceHTTTValid {

  public ServiceHTTTInvalid5(String url, RegistryClient registryClient) {
    super(url, registryClient);
  }

  @Override
  protected EwpHttpSigRequestAuthorizer newAuthorizer() {
    return new EwpHttpSigRequestAuthorizer(this.registryClient) {
      @Override
      protected Http4xx newHttpSig401() {
        Http4xx error = super.newHttpSig401();
        error.removeEwpErrorResponseHeader("Want-Digest");
        return error;
      }
    };
  }
}
