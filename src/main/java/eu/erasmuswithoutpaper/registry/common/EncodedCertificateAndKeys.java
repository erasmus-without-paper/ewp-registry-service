package eu.erasmuswithoutpaper.registry.common;

import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Base64;

import eu.erasmuswithoutpaper.registry.validators.ValidatorKeyStore;

public class EncodedCertificateAndKeys {
  private final String certificateEncoded;
  private final String clientPublicKeyEncoded;
  private final String serverPublicKeyEncoded;

  /**
   * Stores certificate and public keys.
   * @param certificateEncoded
   *     a certificate.
   * @param clientPublicKeyEncoded
   *     a client public key.
   * @param serverPublicKeyEncoded
   *     a server public key.
   */
  public EncodedCertificateAndKeys(String certificateEncoded,
      String clientPublicKeyEncoded, String serverPublicKeyEncoded) {
    this.certificateEncoded = certificateEncoded;
    this.clientPublicKeyEncoded = clientPublicKeyEncoded;
    this.serverPublicKeyEncoded = serverPublicKeyEncoded;
  }

  /**
   * @param validatorKeyStore
   *     a ValidatorKeyStore containing a certificate and public keys.
   * @throws CertificateEncodingException
   *     when problems with encoding appear.
   */
  public EncodedCertificateAndKeys(ValidatorKeyStore validatorKeyStore)
      throws CertificateEncodingException {
    this(
        encodeCertificate(validatorKeyStore.getTlsClientCertificateInUse()),
        encodePublicKey(validatorKeyStore.getClientRsaPublicKeyInUse()),
        encodePublicKey(validatorKeyStore.getServerRsaPublicKeyInUse())
    );
  }

  public String getCertificateEncoded() {
    return certificateEncoded;
  }

  public String getClientPublicKeyEncoded() {
    return clientPublicKeyEncoded;
  }

  public String getServerPublicKeyEncoded() {
    return serverPublicKeyEncoded;
  }

  public static String encodeCertificate(X509Certificate certificate)
      throws CertificateEncodingException {
    return encodeAsBase64(certificate.getEncoded());
  }

  public static String encodePublicKey(PublicKey key) {
    return encodeAsBase64(key.getEncoded());
  }

  private static String encodeAsBase64(byte[] bytes) {
    return new String(
        Base64.getEncoder().encode(bytes),
        StandardCharsets.US_ASCII
    );
  }
}