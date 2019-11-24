package eu.erasmuswithoutpaper.registry.common;

import java.security.KeyPair;
import java.security.cert.X509Certificate;

public class KeyPairAndCertificate {
  public KeyPair keyPair;
  public X509Certificate certificate;

  KeyPairAndCertificate(KeyPair keyPair, X509Certificate certificate) {
    this.keyPair = keyPair;
    this.certificate = certificate;
  }
}
