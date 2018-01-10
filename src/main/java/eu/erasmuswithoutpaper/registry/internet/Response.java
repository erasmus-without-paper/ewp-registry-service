package eu.erasmuswithoutpaper.registry.internet;

import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Represents an "advanced" HTTP response.
 */
public class Response {

  private final int status;
  private byte[] body;
  private final HeaderMap headers;

  /**
   * Create a new response, without and headers.
   *
   * @param status The HTTP response status.
   * @param body The response body.
   */
  public Response(int status, byte[] body) {
    this(status, body, new HeaderMap());
  }

  /**
   * Same as {@link #Response(int, byte[])}, but with headers.
   *
   * @param status The HTTP response status.
   * @param body The response body.
   * @param initialHeaders Initial values for response headers.
   */
  public Response(int status, byte[] body, Map<String, String> initialHeaders) {
    this.status = status;
    this.body = body.clone();
    this.headers = new HeaderMap();
    for (Entry<String, String> entry : initialHeaders.entrySet()) {
      this.putHeader(entry.getKey(), entry.getValue());
    }
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
    return this.headers.get(key);
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
      this.headers.put(key, value);
    }
  }

  /**
   * Remove a header.
   *
   * @param key Name of the header to be removed (case-insensitive).
   */
  public void removeHeader(String key) {
    this.headers.remove(key);
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
