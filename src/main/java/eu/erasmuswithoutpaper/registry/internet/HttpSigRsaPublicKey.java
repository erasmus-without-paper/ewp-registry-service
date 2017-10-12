package eu.erasmuswithoutpaper.registry.internet;

import java.security.InvalidKeyException;
import java.security.Signature;
import java.security.SignatureException;
import java.security.interfaces.RSAPublicKey;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import net.adamcin.httpsig.api.Algorithm;
import net.adamcin.httpsig.api.Key;
import net.adamcin.httpsig.ssh.jce.KeyFormat;
import net.adamcin.httpsig.ssh.jce.SSHKey;
import org.apache.commons.codec.digest.DigestUtils;

/**
 * This custom class was needed because, at the time of writing this, {@link SSHKey} didn't allow us
 * to set a valid EWP-compatible keyId.
 */
public class HttpSigRsaPublicKey implements Key {

  private static Set<Algorithm> algorithms;

  static {
    algorithms = new LinkedHashSet<>();
    algorithms.add(Algorithm.RSA_SHA256);
    algorithms = Collections.unmodifiableSet(algorithms);
  }

  private final RSAPublicKey publicKey;
  private final String sha256fingerprint;

  @SuppressWarnings("javadoc")
  public HttpSigRsaPublicKey(RSAPublicKey publicKey) {
    this.publicKey = publicKey;
    this.sha256fingerprint = DigestUtils.sha256Hex(this.publicKey.getEncoded());
  }

  @Override
  public boolean canSign() {
    return false;
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
    return this.sha256fingerprint;
  }

  @Override
  public byte[] sign(Algorithm algorithm, byte[] contentBytes) {
    throw new RuntimeException("Not supported");
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
      signature.initVerify(this.publicKey);
      signature.update(contentBytes);
      return signature.verify(signatureBytes);
    } catch (InvalidKeyException | SignatureException e) {
      throw new RuntimeException(e);
    }
  }

}
