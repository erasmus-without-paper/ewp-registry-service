package eu.erasmuswithoutpaper.registry.internet.sec;

import java.security.KeyPair;
import java.security.interfaces.RSAPublicKey;

/**
 * Describes a client which made a request using an RSA {@link KeyPair} recognized by the EWP
 * Registry Service.
 */
public class EwpClientWithRsaKey extends EwpClient {

  private final RSAPublicKey publicKey;

  /**
   * @param publicKey The public key with which the request was signed. It refers to a specific
   *        client recognized by the EWP Registry Service.
   */
  public EwpClientWithRsaKey(RSAPublicKey publicKey) {
    this.publicKey = publicKey;
  }

  /**
   * @return The public key with which the request was signed. It refers to a specific client
   *         recognized by the EWP Registry Service.
   */
  public RSAPublicKey getRsaPublicKey() {
    return this.publicKey;
  }
}
