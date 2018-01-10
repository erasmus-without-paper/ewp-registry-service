package eu.erasmuswithoutpaper.registry.internet;

import java.security.KeyPair;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.adamcin.httpsig.api.Algorithm;
import net.adamcin.httpsig.api.Authorization;
import net.adamcin.httpsig.api.Challenge;
import net.adamcin.httpsig.api.DefaultKeychain;
import net.adamcin.httpsig.api.Key;
import net.adamcin.httpsig.api.RequestContent;
import net.adamcin.httpsig.api.Signer;
import org.apache.commons.codec.digest.DigestUtils;

/**
 * Represents an "advanced" HTTP response.
 */
public class Response {

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

  private final Request initialRequest;
  private final int status;
  private byte[] body;

  private final Map<String, String> headers;

  /**
   * Create a new response, without and headers.
   *
   * @param initialRequest The original {@link Request} for which this response is being created.
   * @param status The HTTP response status.
   * @param body The response body.
   */
  public Response(Request initialRequest, int status, byte[] body) {
    // WRTODO: is initialRequest necessary?
    this(initialRequest, status, body, Maps.newHashMap());
  }

  /**
   * Same as {@link #Response(Request, int, byte[])}, but with headers.
   *
   * @param initialRequest The original {@link Request} for which this response is being created.
   * @param status The HTTP response status.
   * @param body The response body.
   * @param initialHeaders Initial values for response headers.
   */
  public Response(Request initialRequest, int status, byte[] body,
      Map<String, String> initialHeaders) {
    this.initialRequest = initialRequest;
    this.status = status;
    this.body = body.clone();
    this.headers = Maps.newHashMap();
    for (Entry<String, String> entry : initialHeaders.entrySet()) {
      this.putHeader(entry.getKey(), entry.getValue());
    }
  }

  /**
   * @return Compute a proper (expected) Base64-encoded for the current contents of the body (only
   *         compute, don't change anything within the request).
   */
  public String computeBodyDigest() {
    byte[] binaryDigest = DigestUtils.sha256(this.body);
    return Base64.getEncoder().encodeToString(binaryDigest);
  }

  /**
   * @return The response body, as returned by the server. This is available regardless of the
   *         response status. In case of error responses, it will contain the error response body.
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP")
  public byte[] getBody() {
    return this.body;
  }

  /**
   * @param key Name of the header (case-insensitive).
   * @return Value of the header, or <code>null</code> if not such header exists.
   */
  public String getHeader(String key) {
    return this.headers.get(key.toLowerCase(Locale.US));
  }

  /**
   * @return A map with all headers.
   */
  public Map<String, String> getHeaders() {
    return Collections.unmodifiableMap(this.headers);
  }

  /**
   * @return The HTTP status returned by the endpoint.
   */
  public int getStatus() {
    return this.status;
  }

  /**
   * Add (or replace) a header.
   *
   * @param key Name of the header to be added or replaced (case-insensitive).
   * @param value New value.
   */
  public void putHeader(String key, String value) {
    if (key != null) {
      this.headers.put(key.toLowerCase(Locale.US), value);
    }
  }

  /**
   * Compute and attach a proper Digest header, based on the current response body. This needs to be
   * called explicitly after the body changes.
   */
  public void recomputeAndAttachDigestHeader() {
    this.putHeader("Digest", "SHA-256=" + this.computeBodyDigest());
  }

  /**
   * Sign the response headers with the provided credentials.
   *
   * @param keyId The SHA-256 fingerprint of the {@link KeyPair} used for signing.
   * @param keyPair {@link KeyPair} to be used for signing.
   * @param headersToSign List of headers to be signed.
   */
  public void recomputeAndAttachSignatureHeader(String keyId, KeyPair keyPair,
      List<String> headersToSign) {

    DefaultKeychain keychain = new DefaultKeychain();
    Key kckey = new HttpSigRsaKeyPair(keyId, keyPair);
    keychain.add(kckey);
    Signer signer = new Signer(keychain);
    List<String> headersSigned = new ArrayList<>(headersToSign);
    if (headersSigned.size() == 0) {
      headersSigned.add("date");
    }
    signer.rotateKeys(
        new Challenge("Not verified", headersSigned, Lists.newArrayList(Algorithm.RSA_SHA256)));

    RequestContent.Builder rcb = new RequestContent.Builder();
    rcb.setRequestTarget(this.initialRequest.getMethod(),
        this.initialRequest.getPathPseudoHeader());
    for (Map.Entry<String, String> entry : this.headers.entrySet()) {
      rcb.addHeader(entry.getKey(), entry.getValue());
    }
    RequestContent content = rcb.build();

    Authorization authz = signer.sign(content, headersSigned);
    if (authz != null) {
      this.putHeader("Signature", getSignatureFromAuthorization(authz.getHeaderValue()));
    }
  }

  /**
   * Remove a header.
   *
   * @param key Name of the header to be removed (case-insensitive).
   */
  public void removeHeader(String key) {
    this.headers.remove(key.toLowerCase(Locale.US));
  }

  /**
   * Replace the response body.
   *
   * @param changed New value.
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP2")
  public void setBody(byte[] changed) {
    this.body = changed;
  }
}
