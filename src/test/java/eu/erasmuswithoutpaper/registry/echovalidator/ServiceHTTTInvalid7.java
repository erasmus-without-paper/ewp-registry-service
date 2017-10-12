package eu.erasmuswithoutpaper.registry.echovalidator;

import java.security.interfaces.RSAPublicKey;

import eu.erasmuswithoutpaper.registry.internet.Internet.Request;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

/**
 * This one is invalid, because it accepts server keyIds as valid client keyIds.
 */
public class ServiceHTTTInvalid7 extends ServiceHTTTValid {

  public ServiceHTTTInvalid7(String url, RegistryClient registryClient) {
    super(url, registryClient);
  }

  @Override
  protected RSAPublicKey verifyClientKeyId(Request request, String keyId)
      throws ErrorResponseException {
    RSAPublicKey clientKey = this.registryClient.findRsaPublicKey(keyId);
    if (clientKey == null) {
      throw new ErrorResponseException(
          this.createErrorResponse(request, 403, "Unknown key: " + keyId));
    }
    return clientKey;
  }
}
