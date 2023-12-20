package eu.erasmuswithoutpaper.registry.constraints;

import java.security.interfaces.RSAPublicKey;
import java.util.Collection;

import eu.erasmuswithoutpaper.registry.common.Severity;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

/**
 * This constraint prevents the Manifest from introducing insecure or invalid RSA keys.
 */
public class ClientKeyConstraint extends AbstractRsaKeySecurityConstraint {

  /**
   * @param minKeyLength The minimum required bit length of the client public key. All keys weaker
   *        than this will be removed from the manifest.
   *
   */
  public ClientKeyConstraint(int minKeyLength) {
    super(minKeyLength);
  }

  @Override
  protected FailedConstraintNotice verifyKey(RSAPublicKey publicKey, String keyNumber,
      String heiCovered, RegistryClient registryClient) {
    FailedConstraintNotice notice = super.verifyKey(publicKey, keyNumber, heiCovered,
        registryClient);
    if (notice != null) {
      return notice;
    }

    if (heiCovered != null) {
      Collection<String> heisCoveredByClientKey =
          registryClient.getHeisCoveredByClientKey(publicKey);
      if (!heisCoveredByClientKey.isEmpty()
          && (!heisCoveredByClientKey.contains(heiCovered) || heisCoveredByClientKey.size() > 1)) {
        return new FailedConstraintNotice(Severity.WARNING,
            "The " + this.getKeyName() + " is not unique. One of your keys (" + keyNumber
                + ") is already registered in the network and covers " + heisCoveredByClientKey
                + ". It will not be imported.");
      }
    }
    return null;
  }

  @Override
  protected String getKeyName() {
    return "client public key";
  }

  @Override
  protected String getXPath() {
    return "mf6:host/mf6:client-credentials-in-use/mf6:rsa-public-key";
  }
}
