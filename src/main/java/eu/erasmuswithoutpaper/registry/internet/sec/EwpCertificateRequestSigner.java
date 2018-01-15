package eu.erasmuswithoutpaper.registry.internet.sec;

import java.security.KeyPair;
import java.security.cert.X509Certificate;

import eu.erasmuswithoutpaper.registry.internet.Request;

/**
 * This {@link RequestSigner} signs the requests by making sure that the proper TLS Client
 * Certificate is used during transport.
 */
public class EwpCertificateRequestSigner implements RequestSigner {

  private final X509Certificate clientCert;
  private final KeyPair clientKeyPair;

  /**
   * @param clientCert The client certificate to sign with.
   * @param clientKeyPair The RSA {@link KeyPair} bound with the certificate. (Private key is needed
   *        to sign the request.)
   */
  public EwpCertificateRequestSigner(X509Certificate clientCert, KeyPair clientKeyPair) {
    this.clientCert = clientCert;
    this.clientKeyPair = clientKeyPair;
  }

  @Override
  public void sign(Request request) {
    request.setClientCertificate(this.clientCert, this.clientKeyPair);
  }

}
