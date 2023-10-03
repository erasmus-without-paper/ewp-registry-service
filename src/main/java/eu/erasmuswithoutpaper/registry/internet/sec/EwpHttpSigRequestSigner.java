package eu.erasmuswithoutpaper.registry.internet.sec;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import eu.erasmuswithoutpaper.registry.common.Utils;
import eu.erasmuswithoutpaper.registry.internet.Request;

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
    this.addMissingHeaders(request);
    this.includeDigestHeader(request);
    this.includeAuthorizationHeader(request);
    request.addProcessingNoticeHtml("Request has been signed with HttpSig.");
  }

  @Override
  public String toString() {
    return "EWP HTTP Signature Request Signer";
  }

  private boolean shouldOverrideExistingAuthorization() {
    return true;
  }

  /**
   * Add all extra request headers required by the specs (if these headers have not yet been added
   * previously).
   *
   * @param request The request to add headers to.
   */
  protected void addMissingHeaders(Request request) {
    if (request.getHeader("Host") == null) {
      request.putHeader("Host", this.parseUrl(request).getHost());
    }
    if ((request.getHeader("Date") == null) && (request.getHeader("Original-Date") == null)) {
      request.putHeader("Date", Utils.getCurrentDateInRFC1123());
    }
    if (request.getHeader("X-Request-Id") == null) {
      request.putHeader("X-Request-Id", UUID.randomUUID().toString());
    }
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
  protected void includeAuthorizationHeader(Request request) {
    if ((!this.shouldOverrideExistingAuthorization())
        && (request.getHeader("Authorization") != null)) {
      return;
    }
    DefaultKeychain keychain = new DefaultKeychain();
    Key kckey = new MyHttpSigRsaKeyPair(this.getKeyId(), this.getKeyPair());
    keychain.add(kckey);
    Signer signer = new Signer(keychain);
    List<String> headersBeingSigned = this.getHeadersToSign(request).stream()
        .map(s -> s.toLowerCase(Locale.US)).collect(Collectors.toList());
    if (headersBeingSigned.size() == 0) {
      headersBeingSigned.add("date");
    }
    signer.rotateKeys(new Challenge("Not verified", headersBeingSigned,
        Collections.singletonList(Algorithm.RSA_SHA256)));

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
  protected void includeDigestHeader(Request request) {
    if ((!this.shouldOverrideExistingDigest()) && (request.getHeader("Digest") != null)) {
      return;
    }
    request.putHeader("Digest", "SHA-256=" + Utils.computeDigestBase64(request.getBodyOrEmpty()));
  }

  /**
   * Parse the URL of the request.
   *
   * @param request The request to get the URL from.
   * @return The parsed URL.
   */
  protected URL parseUrl(Request request) {
    try {
      return new URL(request.getUrl());
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * @return True, if the signing process should overwrite existing Digest values.
   */
  protected boolean shouldOverrideExistingDigest() {
    return true;
  }
}
