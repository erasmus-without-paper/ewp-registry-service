package eu.erasmuswithoutpaper.registry.internet;

import java.io.IOException;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.common.collect.Maps;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * This interface will be used by all the other services for accessing resources over the Internet.
 * This allows to easily replace the Internet for tests.
 */
public interface Internet {

  /**
   * Represents an "advanced" HTTP request.
   *
   * <p>
   * Mutable, not thread-safe.
   * </p>
   */
  class Request {
    private String method;
    private String url;
    private Optional<byte[]> body;
    private Optional<List<String>> headers;
    private Optional<X509Certificate> clientCertificate;
    private Optional<KeyPair> keyPair;

    public Request(String method, String url) {
      this.method = method;
      this.url = url;
      this.body = Optional.empty();
      this.headers = Optional.empty();
      this.clientCertificate = Optional.empty();
    }

    public void addHeader(String header) {
      if (!this.headers.isPresent()) {
        this.headers = Optional.of(new ArrayList<String>());
      }
      this.headers.get().add(header);
    }

    public Optional<byte[]> getBody() {
      return body;
    }

    public Optional<X509Certificate> getClientCertificate() {
      return clientCertificate;
    }

    public Optional<List<String>> getHeaders() {
      return headers;
    }

    public Optional<KeyPair> getKeyPair() {
      return keyPair;
    }

    public String getMethod() {
      return method;
    }

    public String getUrl() {
      return url;
    }

    /**
     * @param body Optional request body to be sent along the request (in case of POST requests,
     *        this often contains x-www-form-urlencoded set of parameters).
     */
    public void setBody(byte[] body) {
      this.body = Optional.of(body);
    }

    /**
     * @param clientCertificate If given, then the request will be made with the supplied TLS client
     *        certificate (keep in mind that our {@link Internet} allows only HTTPS connections).
     * @param keyPair The key-pair for which the certificate has been generated.
     */
    public void setClientCertificate(X509Certificate clientCertificate, KeyPair keyPair) {
      this.clientCertificate = Optional.of(clientCertificate);
      this.keyPair = Optional.of(keyPair);
    }

    /**
     * @param headers The list of headers to be sent along the request (in case of POST requests,
     *        this often contains "Content-Type: application/x-www-form-urlencoded").
     */
    public void setHeaders(List<String> headers) {
      this.headers = Optional.of(headers);
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


  /**
   * Represents an "advanced" HTTP response.
   */
  class Response {
    private final int status;
    private final byte[] body;
    private final Map<String, List<String>> headers;

    public Response(int status, byte[] body) {
      this(status, body, Maps.newHashMap());
    }

    public Response(int status, byte[] body, Map<String, List<String>> headers) {
      this.status = status;
      this.body = body.clone();
      this.headers = headers;
    }

    /**
     * @return List of response headers. Each is a string in the "Key: Value" form.
     */
    public Map<String, List<String>> getAllHeaders() {
      return headers;
    }

    /**
     * @return The response body, as returned by the server. This is available regardless of the
     *         response status. In case of error responses, it will contain the error response body.
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP")
    public byte[] getBody() {
      return body;
    }

    /**
     * @return The HTTP status returned by the endpoint.
     */
    public int getStatus() {
      return status;
    }
  }

  /**
   * Fetch the contents of the given URL.
   *
   * @param url The URL at which the contents can be found.
   * @return The contents of the URL, fully loaded to the memory.
   * @throws IOException Numerous reasons, e.g.
   *         <ul>
   *         <li>there was a problem with the transport,</li>
   *         <li>the URL was invalid,</li>
   *         <li>the server responded with a status other than HTTP 200,</li>
   *         <li>if the URL uses a HTTPS scheme and server certificate has expired,</li>
   *         <li>etc.</li>
   *         </ul>
   */
  byte[] getUrl(String url) throws IOException;

  /**
   * Make an "advanced" request at the given URL. This method allows you to send and retrieve much
   * more data that the {@link #getUrl(String)} method does.
   *
   * @param request Description of the request to be made.
   * @return Description of the response returned by the server.
   * @throws IOException When the request is invalid, or the server could not be reached.
   */
  Response makeRequest(Request request) throws IOException;

  /**
   * Enqueue an email for sending from the Registry Service to the given recipients. This method
   * should return immediately and never throw any exceptions.
   *
   * @param recipients A list of email addresses. These will be put into the "To" header of the sent
   *        email.
   * @param subject This will be put into the "Subject" header of the email.
   * @param contents The contents of the email. Plain-text.
   */
  void queueEmail(List<String> recipients, String subject, String contents);
}
