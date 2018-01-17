package eu.erasmuswithoutpaper.registry.echovalidator;

import java.security.KeyPair;
import java.util.List;

import eu.erasmuswithoutpaper.registryclient.RegistryClient;

/**
 * Invalid, because it accepts GET requests. This encryption method doesn't work for GET requests.
 */
public class ServiceSTETInvalid1 extends ServiceSTETValid {

  public ServiceSTETInvalid1(String url, RegistryClient registryClient, List<KeyPair> serverKeys) {
    super(url, registryClient, serverKeys);
  }

  @Override
  protected String[] getAcceptableMethods() {
    return new String[] { "GET", "POST" };
  }
}
