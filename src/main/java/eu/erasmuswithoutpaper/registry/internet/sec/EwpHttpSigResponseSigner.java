package eu.erasmuswithoutpaper.registry.internet.sec;

import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import eu.erasmuswithoutpaper.registry.common.Utils;
import eu.erasmuswithoutpaper.registry.internet.Request;
import eu.erasmuswithoutpaper.registry.internet.Response;

import com.google.common.collect.Lists;
import net.adamcin.httpsig.api.Algorithm;
import net.adamcin.httpsig.api.Authorization;
import net.adamcin.httpsig.api.Challenge;
import net.adamcin.httpsig.api.DefaultKeychain;
import net.adamcin.httpsig.api.Key;
import net.adamcin.httpsig.api.RequestContent;
import net.adamcin.httpsig.api.Signer;
import org.apache.commons.codec.digest.DigestUtils;

/**
 * {@link ResponseSigner}, which signs the responses with HTTP Signatures.
 */
public class EwpHttpSigResponseSigner extends TlsResponseSigner {

  private static String getSignatureFromAuthorization(String authz) {
    // We need this helper because the library we use wasn't optimized for
    // server signatures (signature.toString() produces output which is valid
    // for the Authorization header, but not for the Signature header).
    String result = authz;
    String start = "signature ";
    String prefix = result.substring(0, start.length()).toLowerCase(Locale.US);
    if (prefix.equals(start)) {
      result = result.substring(start.length());
    }
    return result.trim();
  }

  private final KeyPair keyPair;
  private String keyIdCache = null;

  /**
   * @param keyPair {@link KeyPair} to be used for signing.
   */
  public EwpHttpSigResponseSigner(KeyPair keyPair) {
    this.keyPair = keyPair;
  }

  @Override
  public void sign(Request request, Response response) throws Http4xx {
    super.sign(request, response);
    this.includeDateHeaders(response);
    this.includeDigestHeader(response);
    this.includeXRequestIdHeader(request, response);
    this.includeXRequestSignature(request, response);
    this.includeSignatureHeader(request, response);
    request.addProcessingNoticeHtml("Response has been signed with HttpSig.");
  }

  @Override
  public String toString() {
    return "EWP HTTP Signature Response Signer";
  }

  @Override
  public boolean wasRequestedFor(Request request) {
    Set<String> signatureMethods = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    signatureMethods.addAll(Utils.commaSeparatedTokens(request.getHeader("Accept-Signature")));
    return signatureMethods.contains("rsa-sha256");
  }

  /**
   * Get the list of headers to be signed. This may include the special "(request-target)" header,
   * as explained in HTTP Signature specs.
   *
   * @param request The request for which the response has been generated.
   * @param response The response which we will be signing.
   * @return List of header names, case-insensitive.
   */
  protected List<String> getHeadersToSign(Request request, Response response) {
    // By default, all headers will be signed. (In theory, we should exclude headers which
    // may be changed by proxies though, so this may require some tweaks in the future.)
    List<String> result = new ArrayList<>();
    result.add("(request-target)");
    result.addAll(response.getHeaders().keySet());
    return result;
  }

  /**
   * @return The keyId to be used in the Signature header.
   */
  protected String getKeyId() {
    if (this.keyIdCache == null) {
      this.keyIdCache = DigestUtils.sha256Hex(this.keyPair.getPublic().getEncoded());
    }
    return this.keyIdCache;
  }

  /**
   * Include Original-Date header, if not already included.
   *
   * @param response The response to add the headers to.
   */
  protected void includeDateHeaders(Response response) {
    this.includeDateHeaders(response, false, true);
  }

  /**
   * Include Date and/or Original-Date headers, if not already included.
   *
   * @param response The response to add the headers to.
   * @param date True, if Date header should be included.
   * @param originalDate True, if Original-Date header should be included.
   */
  protected void includeDateHeaders(Response response, boolean date, boolean originalDate) {
    String now = Utils.getCurrentDateInRFC1123();
    if (date && (response.getHeader("Date") == null)) {
      response.putHeader("Date", now);
    }
    if (originalDate && (response.getHeader("Original-Date") == null)) {
      response.putHeader("Original-Date", now);
    }
  }

  /**
   * Recompute and include a proper Digest header.
   *
   * @param response The response to which the Digest should be added.
   */
  protected void includeDigestHeader(Response response) {
    if ((!this.shouldOverrideExistingDigest()) && (response.getHeader("Digest") != null)) {
      return;
    }
    response.putHeader("Digest", "SHA-256=" + Utils.computeDigestBase64(response.getBody()));
  }

  /**
   * Recompute and include a proper Signature header in the response.
   *
   * @param request The request for which the response has been generated.
   * @param response The response to which the Signature should be added.
   */
  protected void includeSignatureHeader(Request request, Response response) {
    if ((!this.shouldOverrideExistingSignature()) && (response.getHeader("Signature") != null)) {
      return;
    }
    DefaultKeychain keychain = new DefaultKeychain();
    Key kckey = new MyHttpSigRsaKeyPair(this.getKeyId(), this.keyPair);
    keychain.add(kckey);
    Signer signer = new Signer(keychain);
    List<String> headersSigned = new ArrayList<>(this.getHeadersToSign(request, response));
    if (headersSigned.size() == 0) {
      headersSigned.add("date");
    }
    signer.rotateKeys(
        new Challenge("Not verified", headersSigned, Lists.newArrayList(Algorithm.RSA_SHA256)));

    RequestContent.Builder rcb = new RequestContent.Builder();
    rcb.setRequestTarget(request.getMethod(), request.getPathPseudoHeader());
    for (Map.Entry<String, String> entry : response.getHeaders().entrySet()) {
      rcb.addHeader(entry.getKey().toLowerCase(Locale.US), entry.getValue());
    }
    RequestContent content = rcb.build();

    Authorization authz = signer.sign(content, headersSigned);
    if (authz != null) {
      response.putHeader("Signature", getSignatureFromAuthorization(authz.getHeaderValue()));
    }
  }

  /**
   * Include a proper X-Request-Signature header in the response, based on the HTTP Signature of the
   * original request.
   *
   * @param request The request for which the response is generated.
   * @param response The response to which the X-Request-Signature header should be added.
   */
  protected void includeXRequestSignature(Request request, Response response) {
    if (response.getHeader("X-Request-Signature") != null) {
      return;
    }
    String authzString = request.getHeader("Authorization");
    if (authzString == null) {
      return;
    }
    Authorization authz = Authorization.parse(authzString);
    if (authz == null) {
      return;
    }
    if (authz.getSignature() == null) {
      return;
    }
    response.putHeader("X-Request-Signature", authz.getSignature());
  }

  /**
   * @return True, if {@link #includeDigestHeader(Response)} should overwrite existing Digests.
   */
  protected boolean shouldOverrideExistingDigest() {
    return true;
  }

  /**
   * @return True, if {@link #includeSignatureHeader(Request, Response)} should overwrite existing
   *         Signatures.
   */
  protected boolean shouldOverrideExistingSignature() {
    return true;
  }
}
