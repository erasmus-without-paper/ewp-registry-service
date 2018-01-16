package eu.erasmuswithoutpaper.registry.echovalidator;

import java.security.interfaces.RSAPublicKey;

import eu.erasmuswithoutpaper.registry.internet.sec.EwpHttpSigRequestAuthorizer;
import eu.erasmuswithoutpaper.registry.internet.sec.Http4xx;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

/**
 * This one is invalid, because it accepts server keyIds as valid client keyIds.
 */
public class ServiceHTTTInvalid7 extends ServiceHTTTValid {

  public ServiceHTTTInvalid7(String url, RegistryClient registryClient) {
    super(url, registryClient);
  }

  @Override
  protected EwpHttpSigRequestAuthorizer newAuthorizer() {
    return new EwpHttpSigRequestAuthorizer(this.registryClient) {
      @Override
      protected RSAPublicKey verifyClientKeyId(String keyId) throws Http4xx {
        RSAPublicKey clientKey = this.registryClient.findRsaPublicKey(keyId);
        if (clientKey == null) {
          throw new Http4xx(403, "Unknown key: " + keyId);
        }
        return clientKey;
      }
    };
  }
}
