package eu.erasmuswithoutpaper.registry.internet.sec;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

import eu.erasmuswithoutpaper.registry.documentbuilder.KnownNamespace;
import eu.erasmuswithoutpaper.registry.internet.HeaderMap;
import eu.erasmuswithoutpaper.registry.internet.Response;

import org.apache.commons.lang.StringEscapeUtils;

/**
 * Thrown by signers and authorizers when the input supplied by the external client is invalid in
 * some way (or doesn't match what the signer or authenticator expects).
 */
@SuppressWarnings("serial")
public class Http4xx extends Exception {

  private final int statusCode;
  private final String developerMessage;
  private final HeaderMap headers;

  /**
   * @param statusCode HTTP status code to be returned to the client.
   * @param developerMessage The message describing the error which the client seems to have made.
   */
  public Http4xx(int statusCode, String developerMessage) {
    super("HTTP " + statusCode + ": " + developerMessage);
    this.statusCode = statusCode;
    this.developerMessage = developerMessage;
    this.headers = new HeaderMap();
    this.headers.put("Content-Type", "text/xml; charset=utf-8");
  }

  /**
   * @return A {@link Response} with EWP-formatted error-response contents.
   */
  public Response generateEwpErrorResponse() {
    StringBuilder sb = new StringBuilder();
    String ns = KnownNamespace.COMMON_TYPES_V1.getNamespaceUri();
    sb.append("<error-response xmlns='").append(ns).append("'>");
    sb.append("<developer-message>");
    sb.append(StringEscapeUtils.escapeXml(this.developerMessage));
    sb.append("</developer-message>");
    sb.append("</error-response>");
    byte[] body = sb.toString().getBytes(StandardCharsets.UTF_8);
    return new Response(this.statusCode, body, new HeaderMap(this.headers));
  }

  /**
   * @return Unmodifiable map of error response headers (added via
   *         {@link #putEwpErrorResponseHeader(String, String)}).
   */
  public Map<String, String> getHeaders() {
    return Collections.unmodifiableMap(this.headers);
  }

  /**
   * @return HTTP status code for the error response.
   */
  public int getStatusCode() {
    return this.statusCode;
  }

  /**
   * Add an additional header to the error response which {@link #generateEwpErrorResponse()} will
   * generate.
   *
   * @param key Name of the header.
   * @param value Value for the header.
   */
  public void putEwpErrorResponseHeader(String key, String value) {
    this.headers.put(key, value);
  }

  /**
   * Remove a header from the response which {@link #generateEwpErrorResponse()} will generate.
   *
   * @param key The name of the header to be removed.
   */
  public void removeEwpErrorResponseHeader(String key) {
    this.headers.remove(key);
  }
}
