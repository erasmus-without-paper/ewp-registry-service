package eu.erasmuswithoutpaper.registry.echovalidator;

import java.security.KeyPair;
import java.util.List;

import eu.erasmuswithoutpaper.registryclient.RegistryClient;

/**
 * This one is invalid, because it doesn't sign the digest header.
 */
public class ServiceMMTTInvalid8 extends ServiceMMTTValid {

  public ServiceMMTTInvalid8(String url, RegistryClient registryClient, String myKeyId,
      KeyPair myKeyPair) {
    super(url, registryClient, myKeyId, myKeyPair);
  }

  @Override
  protected List<String> getHeaderCandidatesToSign() {
    List<String> result = super.getHeaderCandidatesToSign();
    result.remove("digest");
    return result;
  }
}
