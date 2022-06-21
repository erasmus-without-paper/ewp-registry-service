package eu.erasmuswithoutpaper.registry.constraints;

/**
 * This constraint prevents the Manifest from introducing insecure or invalid RSA keys.
 */
public class ServerKeySecurityConstraint extends AbstractRsaKeySecurityConstraint {

  /**
   * @param minKeyLength The minimum required bit length of the client public key. All keys weaker
   *        than this will be removed from the manifest.
   */
  public ServerKeySecurityConstraint(int minKeyLength) {
    super(minKeyLength);
  }

  @Override
  protected String getKeyName() {
    return "server public key";
  }

  @Override
  protected String getXPath() {
    return "mf5:host/mf5:server-credentials-in-use/mf5:rsa-public-key | "
        + "mf6:host/mf6:server-credentials-in-use/mf6:rsa-public-key";
  }
}
