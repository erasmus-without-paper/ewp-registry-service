package eu.erasmuswithoutpaper.registry.internet;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

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
  private Optional<KeyPair> clientCertificateKeyPair;
  private final List<String> processingNoticesHtml;

  /**
   * Make a snapshot of other request.
   *
   * @param other The request to copy properties from.
   */
  public Request(Request other) {
    this.method = other.method;
    this.url = other.url;
    if (other.body.isPresent()) {
      this.body = Optional.of(other.body.get().clone());
    } else {
      this.body = Optional.empty();
    }
    this.headers = new HeaderMap(other.headers);
    if (other.clientCertificate.isPresent()) {
      this.clientCertificate = Optional.of(other.clientCertificate.get());
    } else {
      this.clientCertificate = Optional.empty();
    }
    if (other.clientCertificateKeyPair.isPresent()) {
      this.clientCertificateKeyPair = Optional.of(other.clientCertificateKeyPair.get());
    } else {
      this.clientCertificateKeyPair = Optional.empty();
    }
    this.processingNoticesHtml = new ArrayList<>(other.processingNoticesHtml);
  }

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
    this.clientCertificateKeyPair = Optional.empty();
    this.processingNoticesHtml = new ArrayList<>();
    if (Objects.equals(method, "POST") || Objects.equals(method, "PUT")) {
      this.headers.put("Content-Length", "0");
    }
  }

  /**
   * @param message Notice to be later returned in {@link #getProcessingNoticesHtml()}.
   */
  public void addProcessingNoticeHtml(String message) {
    this.processingNoticesHtml.add(message);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (this.getClass() != obj.getClass()) {
      return false;
    }
    Request other = (Request) obj;
    if (this.body.isPresent() != other.body.isPresent()) {
      return false;
    }
    if (this.body.isPresent() && !Arrays.equals(this.body.get(), other.body.get())) {
      return false;
    }
    if (this.headers == null) {
      if (other.headers != null) {
        return false;
      }
    } else if (!this.headers.equals(other.headers)) {
      return false;
    }
    if (this.method == null) {
      if (other.method != null) {
        return false;
      }
    } else if (!this.method.equals(other.method)) {
      return false;
    }
    if (this.processingNoticesHtml == null) {
      if (other.processingNoticesHtml != null) {
        return false;
      }
    } else if (!this.processingNoticesHtml.equals(other.processingNoticesHtml)) {
      return false;
    }
    if (this.url == null) {
      if (other.url != null) {
        return false;
      }
    } else if (!this.url.equals(other.url)) {
      return false;
    }
    return true;
  }

  /**
   * @return Request body, if present.
   */
  public Optional<byte[]> getBody() {
    return this.body;
  }

  /**
   * @return Either a request body, or an empty byte array if no request body was present.
   */
  public byte[] getBodyOrEmpty() {
    if (this.body.isPresent()) {
      return this.body.get();
    } else {
      return new byte[0];
    }
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
    return this.clientCertificateKeyPair;
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
   * @return The list of notices, which should be visible to the user debugging their requests and
   *         responses. For example, it may say that some headers have been removed during the
   *         authorization process, because they were not signed.
   */
  public List<String> getProcessingNoticesHtml() {
    return Collections.unmodifiableList(this.processingNoticesHtml);
  }

  /**
   * @return The URL at which this request is targeted.
   */
  public String getUrl() {
    return this.url;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((this.body == null) ? 0 : this.body.hashCode());
    result = prime * result + ((this.headers == null) ? 0 : this.headers.hashCode());
    result = prime * result + ((this.method == null) ? 0 : this.method.hashCode());
    result = prime * result
        + ((this.processingNoticesHtml == null) ? 0 : this.processingNoticesHtml.hashCode());
    result = prime * result + ((this.url == null) ? 0 : this.url.hashCode());
    return result;
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
   * @param body Optional request body to be sent along the request (in case of POST requests, this
   *        often contains x-www-form-urlencoded set of parameters). This method also sets
   *        'Content-Length' header.
   */
  public void setBodyAndContentLength(byte[] body) {
    setBody(body);
    if (Objects.equals(this.getMethod(), "POST")
        || Objects.equals(this.getMethod(), "PUT")) {
      int length = 0;
      if (this.body.isPresent()) {
        length = this.body.get().length;
      }
      this.putHeader("Content-Length", Integer.toString(length));
    }
  }

  /**
   * @param clientCertificate If not null, then it indicates that the request was made (or will be
   *        made) with the supplied TLS client certificate (keep in mind that our {@link Internet}
   *        allows only HTTPS connections).
   * @param keyPair The key-pair for which the certificate has been generated. This will be
   *        <code>null</code> in case of requests received from outside, but it MUST be set if the
   *        request originates from ourselves (we are signing it with our own certificate).
   */
  public void setClientCertificate(X509Certificate clientCertificate, KeyPair keyPair) {
    this.clientCertificate = Optional.ofNullable(clientCertificate);
    this.clientCertificateKeyPair = Optional.ofNullable(keyPair);
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
