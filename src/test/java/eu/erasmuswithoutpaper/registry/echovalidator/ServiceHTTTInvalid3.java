package eu.erasmuswithoutpaper.registry.echovalidator;

import java.util.List;

import eu.erasmuswithoutpaper.registryclient.RegistryClient;

/**
 * This one is invalid, because it doesn't sign the X-Request-Id header.
 */
public class ServiceHTTTInvalid3 extends ServiceHTTTValid {

  public ServiceHTTTInvalid3(String url, RegistryClient registryClient) {
    super(url, registryClient);
  }

  @Override
  protected List<String> getHeadersThatNeedToBeSignedExcludingDates() {
    List<String> result = super.getHeadersThatNeedToBeSignedExcludingDates();
    result.remove("x-request-id");
    return result;
  }
}
