package eu.erasmuswithoutpaper.registry.internet.sec;

import java.security.cert.X509Certificate;

/**
 * Describes a client which made a request using a TLS Client Certificate recognized by the EWP
 * Registry Service.
 */
public class EwpClientWithCertificate extends ClientInfo {

  private final X509Certificate cert;

  /**
   * @param cert The TLS certificate used by the client for making the request.
   */
  public EwpClientWithCertificate(X509Certificate cert) {
    this.cert = cert;
  }

  /**
   * @return The TLS certificate used by the client for making the request.
   */
  public X509Certificate getCertificate() {
    return this.cert;
  }
}
