package eu.erasmuswithoutpaper.registry.internet.sec;

import java.security.KeyPair;
import java.security.interfaces.RSAPublicKey;

/**
 * Describes a server which signed the response using an RSA {@link KeyPair} recognized by the EWP
 * Registry Service.
 */
public class EwpServerWithRsaKey extends EwpServer {

  private final RSAPublicKey publicKey;

  /**
   * @param publicKey The public key with which the response was signed. It refers to a specific
   *        server recognized by the EWP Registry Service.
   */
  public EwpServerWithRsaKey(RSAPublicKey publicKey) {
    this.publicKey = publicKey;
  }

  /**
   * @return The public key with which the response was signed. It refers to a specific server
   *         recognized by the EWP Registry Service.
   */
  public RSAPublicKey getRsaPublicKey() {
    return this.publicKey;
  }
}
