package eu.erasmuswithoutpaper.registry.internet.sec;

import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import eu.erasmuswithoutpaper.registry.common.Utils;
import eu.erasmuswithoutpaper.registry.internet.Request;

import com.google.common.collect.Lists;
import net.adamcin.httpsig.api.Algorithm;
import net.adamcin.httpsig.api.Authorization;
import net.adamcin.httpsig.api.Challenge;
import net.adamcin.httpsig.api.DefaultKeychain;
import net.adamcin.httpsig.api.Key;
import net.adamcin.httpsig.api.RequestContent;
import net.adamcin.httpsig.api.Signer;

/**
 * This {@link RequestSigner} signs requests with EWP HTTP Signatures.
 */
public class EwpHttpSigRequestSigner implements RequestSigner {

  /**
   * The key pair to be used for signing.
   */
  protected final KeyPair keyPair;

  /**
   * Cached SHA-256 hex fingerprint of the {@link #keyPair}.
   */
  protected String keyIdCache;

  /**
   * @param keyPair The key pair to be used for signing.
   */
  public EwpHttpSigRequestSigner(KeyPair keyPair) {
    this.keyPair = keyPair;

  }

  /**
   * @return The value to be used as HTTP Signature's keyId.
   */
  public String getKeyId() {
    if (this.keyIdCache == null) {
      this.keyIdCache = Utils.computeDigestHex(this.keyPair.getPublic().getEncoded());
    }
    return this.keyIdCache;
  }

  /**
   * @return The key pair which the signer is using for signing.
   */
  public KeyPair getKeyPair() {
    return this.keyPair;
  }

  @Override
  public void sign(Request request) {
    this.recomputeAndAttachDigestHeader(request);
    this.recomputeAndAttachAuthorizationHeader(request);
  }

  /**
   * Get the list of headers to be signed. This may include the special "(request-target)" header,
   * as explained in HTTP Signature specs.
   *
   * @param request The request which we will be signing.
   * @return List of header names, case-insensitive.
   */
  protected List<String> getHeadersToSign(Request request) {
    // By default, all headers will be signed.
    List<String> result = new ArrayList<>();
    result.add("(request-target)");
    result.addAll(request.getHeaders().keySet());
    return result;
  }

  /**
   * Compute a proper request's signature and attach it in the request's Authorization header.
   *
   * @param request The request to process.
   */
  protected void recomputeAndAttachAuthorizationHeader(Request request) {
    DefaultKeychain keychain = new DefaultKeychain();
    Key kckey = new HttpSigRsaKeyPair(this.getKeyId(), this.getKeyPair());
    keychain.add(kckey);
    Signer signer = new Signer(keychain);
    List<String> headersBeingSigned = this.getHeadersToSign(request).stream()
        .map(s -> s.toLowerCase(Locale.US)).collect(Collectors.toList());
    if (headersBeingSigned.size() == 0) {
      headersBeingSigned.add("date");
    }
    signer.rotateKeys(new Challenge("Not verified", headersBeingSigned,
        Lists.newArrayList(Algorithm.RSA_SHA256)));

    RequestContent.Builder rcb = new RequestContent.Builder();
    rcb.setRequestTarget(request.getMethod(), request.getPathPseudoHeader());
    for (Map.Entry<String, String> entry : request.getHeaders().entrySet()) {
      rcb.addHeader(entry.getKey(), entry.getValue());
    }
    RequestContent content = rcb.build();

    Authorization authz = signer.sign(content, headersBeingSigned);
    if (authz == null) {
      throw new RuntimeException("Could not sign");
    }
    request.putHeader("Authorization", authz.getHeaderValue());
  }

  /**
   * Compute a SHA-256 digest of the request's body, and attach it to the request's Digest header.
   * Will override any previously set Digests. This needs to be called explicitly, after the body is
   * changed.
   *
   * @param request The request to be processed.
   */
  protected void recomputeAndAttachDigestHeader(Request request) {
    request.putHeader("Digest", "SHA-256=" + Utils.computeDigestBase64(request.getBodyOrEmpty()));
  }
}
