package eu.erasmuswithoutpaper.registry.echovalidator;

import java.security.KeyPair;
import java.util.List;

import eu.erasmuswithoutpaper.registryclient.RegistryClient;

import org.assertj.core.util.Lists;

/**
 * Invalid, because it doesn't accept requests encrypted to its own server keys.
 */
public class ServiceSTETInvalid2 extends ServiceSTETValid {

  public ServiceSTETInvalid2(String url, RegistryClient registryClient, List<KeyPair> serverKeys) {
    super(url, registryClient, Lists.newArrayList());
  }
}
