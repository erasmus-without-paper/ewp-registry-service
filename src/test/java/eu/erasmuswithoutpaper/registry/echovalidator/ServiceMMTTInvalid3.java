package eu.erasmuswithoutpaper.registry.echovalidator;

import java.security.KeyPair;

import eu.erasmuswithoutpaper.registry.internet.Response;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

/**
 * This one is invalid, because it includes Digests in unknown algorithm.
 */
public class ServiceMMTTInvalid3 extends ServiceMMTTValid {

  public ServiceMMTTInvalid3(String url, RegistryClient registryClient, String myKeyId,
      KeyPair myKeyPair) {
    super(url, registryClient, myKeyId, myKeyPair);
  }

  @Override
  protected void includeDigestHeader(Response response) {
    response.putHeader("Digest", "Unknown-Algorithm=Value");
  }
}
