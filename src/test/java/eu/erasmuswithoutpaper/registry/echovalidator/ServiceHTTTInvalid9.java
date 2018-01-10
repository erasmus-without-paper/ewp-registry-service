package eu.erasmuswithoutpaper.registry.echovalidator;

import java.security.interfaces.RSAPublicKey;

import eu.erasmuswithoutpaper.registry.internet.Request;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

import net.adamcin.httpsig.api.Authorization;

/**
 * This one is invalid, because it doesn't verify the signature.
 */
public class ServiceHTTTInvalid9 extends ServiceHTTTValid {

  public ServiceHTTTInvalid9(String url, RegistryClient registryClient) {
    super(url, registryClient);
  }

  @Override
  protected void verifySignature(Request request, RSAPublicKey expectedClientKey,
      Authorization authz) throws ErrorResponseException {
    return;
  }
}
