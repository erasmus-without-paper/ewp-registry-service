package eu.erasmuswithoutpaper.registry.consoleapplication;

import java.security.KeyPair;
import java.security.cert.Certificate;

public class KeyPairAndCertificate {
  public KeyPair keyPair;
  public Certificate certificate;

  KeyPairAndCertificate(KeyPair keyPair, Certificate certificate) {
    this.keyPair = keyPair;
    this.certificate = certificate;
  }
}
