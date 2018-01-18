package eu.erasmuswithoutpaper.registry.internet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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
  private final List<String> processingNoticesHtml;

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
    this.processingNoticesHtml = new ArrayList<>();
  }

  /**
   * Make a snapshot copy of some other response.
   *
   * @param other Other response to copy values from.
   */
  public Response(Response other) {
    this.status = other.status;
    this.body = other.body.clone();
    this.headers = new HeaderMap(other.headers);
    this.processingNoticesHtml = new ArrayList<>(other.processingNoticesHtml);
  }

  /**
   * @param message Notice to be added to {@link #getProcessingNoticesHtml()}.
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
    Response other = (Response) obj;
    if (!Arrays.equals(this.body, other.body)) {
      return false;
    }
    if (this.headers == null) {
      if (other.headers != null) {
        return false;
      }
    } else if (!this.headers.equals(other.headers)) {
      return false;
    }
    if (this.processingNoticesHtml == null) {
      if (other.processingNoticesHtml != null) {
        return false;
      }
    } else if (!this.processingNoticesHtml.equals(other.processingNoticesHtml)) {
      return false;
    }
    if (this.status != other.status) {
      return false;
    }
    return true;
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
   * @return The list of notices, which should be visible to the user debugging their requests and
   *         responses. For example, it may say that some headers have been removed during the
   *         authorization process, because they were not signed.
   */
  public List<String> getProcessingNoticesHtml() {
    return Collections.unmodifiableList(this.processingNoticesHtml);
  }

  /**
   * @return The HTTP status returned by the endpoint.
   */
  public int getStatus() {
    return this.status;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + Arrays.hashCode(this.body);
    result = prime * result + ((this.headers == null) ? 0 : this.headers.hashCode());
    result = prime * result
        + ((this.processingNoticesHtml == null) ? 0 : this.processingNoticesHtml.hashCode());
    result = prime * result + this.status;
    return result;
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
