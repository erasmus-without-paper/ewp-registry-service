package eu.erasmuswithoutpaper.registry.echovalidator;

import eu.erasmuswithoutpaper.registry.internet.Request;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

/**
 * Invalid, because it doesn't verify the digest.
 */
public class ServiceHTTTInvalid10 extends ServiceHTTTValid {

  public ServiceHTTTInvalid10(String url, RegistryClient registryClient) {
    super(url, registryClient);
  }

  @Override
  protected void verifyDigestHeader(Request request) throws ErrorResponseException {
    return;
  }

}
