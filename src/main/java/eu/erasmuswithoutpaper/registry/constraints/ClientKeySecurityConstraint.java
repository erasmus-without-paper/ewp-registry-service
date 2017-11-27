package eu.erasmuswithoutpaper.registry.constraints;

/**
 * This constraint prevents the Manifest from introducing insecure or invalid RSA keys.
 */
public class ClientKeySecurityConstraint extends AbstractRsaKeySecurityConstraint {

  /**
   * @param minKeyLength The minimum required bit length of the client public key. All keys weaker
   *        than this will be removed from the manifest.
   */
  public ClientKeySecurityConstraint(int minKeyLength) {
    super(minKeyLength);
  }

  @Override
  protected String getKeyName() {
    return "client public key";
  }

  @Override
  protected String getXPath() {
    return "mf4:client-credentials-in-use/mf4:rsa-public-key";
  }
}
