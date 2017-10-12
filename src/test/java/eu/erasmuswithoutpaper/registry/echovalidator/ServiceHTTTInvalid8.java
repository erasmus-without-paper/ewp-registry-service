package eu.erasmuswithoutpaper.registry.echovalidator;

import eu.erasmuswithoutpaper.registryclient.RegistryClient;

/**
 * Invalid because it doesn't verify the Date's threshold.
 */
public class ServiceHTTTInvalid8 extends ServiceHTTTValid {

  public ServiceHTTTInvalid8(String url, RegistryClient registryClient) {
    super(url, registryClient);
  }

  @Override
  protected String findErrorsInDateHeader(String dateValue) {
    return null;
  }

}
