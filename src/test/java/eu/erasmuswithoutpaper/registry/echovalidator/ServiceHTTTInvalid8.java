package eu.erasmuswithoutpaper.registry.echovalidator;

import eu.erasmuswithoutpaper.registry.internet.sec.EwpHttpSigRequestAuthorizer;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

/**
 * Invalid because it doesn't verify the Date's threshold.
 */
public class ServiceHTTTInvalid8 extends ServiceHTTTValid {

  public ServiceHTTTInvalid8(String url, RegistryClient registryClient) {
    super(url, registryClient);
  }

  @Override
  protected EwpHttpSigRequestAuthorizer newAuthorizer() {
    return new EwpHttpSigRequestAuthorizer(this.registryClient) {
      @Override
      protected String findErrorsInDateHeader(String dateValue) {
        return null;
      }
    };
  }

}
