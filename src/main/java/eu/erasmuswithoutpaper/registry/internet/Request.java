package eu.erasmuswithoutpaper.registry.internet;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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
 * Represents an abstract HTTP request.
 *
 * <p>
 * Mutable, not thread-safe.
 * </p>
 */
public class Request {

  private String method;
  private String url;
  private Optional<byte[]> body;
  private final HeaderMap headers;
  private Optional<X509Certificate> clientCertificate;
  private Optional<KeyPair> keyPair;

  /**
   * Construct a new (somewhat empty) request.
   *
   * @param method Upper-case HTTP method (e.g. "POST").
   * @param url The URL requested by this HTTP request.
   */
  public Request(String method, String url) {
    this.method = method;
    this.url = url;
    this.body = Optional.empty();
    this.headers = new HeaderMap();
    this.clientCertificate = Optional.empty();
  }

  /**
   * @return Base64-encoded SHA-256 digest of request's body (if there's no body, then it's a digest
   *         of an empty byte-array).
   */
  public String computeBodyDigest() {
    byte[] body;
    if (this.getBody().isPresent()) {
      body = this.getBody().get();
    } else {
      body = new byte[0];
    }
    byte[] binaryDigest = DigestUtils.sha256(body);
    return Base64.getEncoder().encodeToString(binaryDigest);
  }

  /**
   * @return Request body, if present.
   */
  public Optional<byte[]> getBody() {
    return this.body;
  }

  /**
   * @return Client certificate which has been used for TLS transport (if it has been used).
   */
  public Optional<X509Certificate> getClientCertificate() {
    return this.clientCertificate;
  }

  /**
   * @return {@link KeyPair} which has been used for signing this request (present only if the
   *         request is signed by ourselves).
   */
  public Optional<KeyPair> getClientCertificateKeyPair() {
    return this.keyPair;
  }

  /**
   * @param key The name of the header (case insensitive).
   * @return The value of this header, or <code>null</code>, if no such header is present.
   */
  public String getHeader(String key) {
    return this.headers.get(key);
  }

  /**
   * @return A map of all headers.
   */
  public Map<String, String> getHeaders() {
    return Collections.unmodifiableMap(this.headers);
  }

  /**
   * @return HTTP method used in this request (upper-case).
   */
  public String getMethod() {
    return this.method;
  }

  /**
   * Extract path from request's URL.
   *
   * @return ":path" pseudoheader, as specified in HTTP/2, Section 8.1.2.3
   *         (https://tools.ietf.org/html/rfc7540#section-8.1.2.3).
   */
  public String getPathPseudoHeader() {
    try {
      URL parsed = new URL(this.url);
      StringBuilder sb = new StringBuilder();
      sb.append(parsed.getPath());
      if (parsed.getQuery() != null) {
        sb.append('?');
        sb.append(parsed.getQuery());
      }
      if (sb.length() == 0) {
        sb.append('/');
      }
      return sb.toString();
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * @return The URL at which this request is targeted.
   */
  public String getUrl() {
    return this.url;
  }

  /**
   * Put (and possibly replace) a new header.
   *
   * @param key The name of the header to put (case-insensitive).
   * @param value The (new) value.
   */
  public void putHeader(String key, String value) {
    this.headers.put(key, value);
  }

  /**
   * Analyze the current body, and attach a valid Digest header with the SHA-256 digest of this
   * body. This needs to be called explicitly, after the body is changed.
   */
  public void recomputeAndAttachDigestHeader() {
    this.putHeader("Digest", "SHA-256=" + this.computeBodyDigest());
  }

  /**
   * Analyze the current headers, and sign them with the given credentials. The needs to be called
   * explicitly, after the headers are changed.
   *
   * @param keyId SHA-256 fingerprint of the {@link KeyPair} with which we will be signing.
   * @param keyPair The {@link KeyPair} with which we will be signing.
   * @param headersToSign The list of headers to be signed (case-insensitive).
   */
  public void recomputeAndAttachHttpSigAuthorizationHeader(String keyId, KeyPair keyPair,
      List<String> headersToSign) {

    DefaultKeychain keychain = new DefaultKeychain();
    Key kckey = new HttpSigRsaKeyPair(keyId, keyPair);
    keychain.add(kckey);
    Signer signer = new Signer(keychain);
    List<String> headersBeingSigned =
        headersToSign.stream().map(s -> s.toLowerCase(Locale.US)).collect(Collectors.toList());
    if (headersBeingSigned.size() == 0) {
      headersBeingSigned.add("date");
    }
    signer.rotateKeys(new Challenge("Not verified", headersBeingSigned,
        Lists.newArrayList(Algorithm.RSA_SHA256)));

    RequestContent.Builder rcb = new RequestContent.Builder();
    rcb.setRequestTarget(this.getMethod(), this.getPathPseudoHeader());
    for (Map.Entry<String, String> entry : this.headers.entrySet()) {
      rcb.addHeader(entry.getKey(), entry.getValue());
    }
    RequestContent content = rcb.build();

    Authorization authz = signer.sign(content, headersBeingSigned);
    if (authz == null) {
      throw new RuntimeException("Could not sign");
    }
    this.putHeader("Authorization", authz.getHeaderValue());
  }

  /**
   * @param key The name of the header to be removed (case-insensitive).
   */
  public void removeHeader(String key) {
    this.headers.remove(key);
  }

  /**
   * @param body Optional request body to be sent along the request (in case of POST requests, this
   *        often contains x-www-form-urlencoded set of parameters).
   */
  public void setBody(byte[] body) {
    this.body = Optional.ofNullable(body);
  }

  /**
   * @param clientCertificate If given, then the request will be made with the supplied TLS client
   *        certificate (keep in mind that our {@link Internet} allows only HTTPS connections).
   * @param keyPair The key-pair for which the certificate has been generated.
   */
  public void setClientCertificate(X509Certificate clientCertificate, KeyPair keyPair) {
    this.clientCertificate = Optional.ofNullable(clientCertificate);
    this.keyPair = Optional.ofNullable(keyPair);
  }

  /**
   * @param method Request method to be used (e.g. "GET", "POST", "PUT").
   */
  public void setMethod(String method) {
    this.method = method;
  }

  /**
   * @param url The endpoint to be called.
   */
  public void setUrl(String url) {
    this.url = url;
  }
}
