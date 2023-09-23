package eu.erasmuswithoutpaper.registry.internet.sec;

import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.Signature;
import java.security.SignatureException;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import net.adamcin.httpsig.api.Algorithm;
import net.adamcin.httpsig.api.Key;
import net.adamcin.httpsig.ssh.jce.KeyFormat;

/**
 * This custom class was needed because, at the time of writing this,
 * {@link net.adamcin.httpsig.ssh.jce.SSHKey} didn't allow us to set a valid EWP-compatible keyId.
 */
public class MyHttpSigRsaKeyPair implements Key {

  private static Set<Algorithm> algorithms;

  static {
    algorithms = new LinkedHashSet<>();
    algorithms.add(Algorithm.RSA_SHA256);
    algorithms = Collections.unmodifiableSet(algorithms);
  }

  private final String keyId;
  private final KeyPair keyPair;

  @SuppressWarnings("javadoc")
  public MyHttpSigRsaKeyPair(String keyId, KeyPair keyPair) {
    this.keyId = keyId;
    this.keyPair = keyPair;
  }

  @Override
  public boolean canSign() {
    return true;
  }

  @Override
  public boolean canVerify() {
    return true;
  }

  @Override
  public Set<Algorithm> getAlgorithms() {
    return algorithms;
  }

  @Override
  public String getId() {
    return this.keyId;
  }

  @Override
  public byte[] sign(Algorithm algorithm, byte[] contentBytes) {
    if (contentBytes == null) {
      throw new IllegalArgumentException("contentBytes cannot be null.");
    }
    Signature signature = KeyFormat.SSH_RSA.getSignatureInstance(algorithm);
    try {
      signature.initSign(this.keyPair.getPrivate());
      signature.update(contentBytes);
      return signature.sign();
    } catch (InvalidKeyException | SignatureException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public boolean verify(Algorithm algorithm, byte[] contentBytes, byte[] signatureBytes) {
    if (contentBytes == null) {
      throw new IllegalArgumentException("contentBytes cannot be null.");
    }

    if (signatureBytes == null) {
      throw new IllegalArgumentException("signatureBytes cannot be null.");
    }

    Signature signature = KeyFormat.SSH_RSA.getSignatureInstance(algorithm);
    try {
      signature.initVerify(this.keyPair.getPublic());
      signature.update(contentBytes);
      return signature.verify(signatureBytes);
    } catch (InvalidKeyException | SignatureException e) {
      throw new RuntimeException(e);
    }
  }

}
