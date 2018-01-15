package eu.erasmuswithoutpaper.registry.internet.sec;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import eu.erasmuswithoutpaper.registry.documentbuilder.KnownNamespace;
import eu.erasmuswithoutpaper.registry.internet.HeaderMap;
import eu.erasmuswithoutpaper.registry.internet.Response;

import org.apache.commons.lang.StringEscapeUtils;

/**
 * Thrown by signers and authenticators when the input supplied by the external client is invalid in
 * some way (or doesn't match what the signer or authenticator expects).
 */
@SuppressWarnings("serial")
public class Http4xx extends Exception {

  private final int statusCode;
  private final String developerMessage;
  private final Map<String, String> extraResponseHeaders;

  /**
   * @param statusCode HTTP status code to be returned to the client.
   * @param developerMessage The message describing the error which the client seems to be have
   *        made.
   */
  public Http4xx(int statusCode, String developerMessage) {
    super("HTTP " + statusCode + ": " + developerMessage);
    this.statusCode = statusCode;
    this.developerMessage = developerMessage;
    this.extraResponseHeaders = new HashMap<>();
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

    HeaderMap headers = new HeaderMap();
    headers.put("Content-Type", "text/xml; charset=utf-8");
    headers.putAll(this.extraResponseHeaders);
    return new Response(this.statusCode, body, headers);
  }

  /**
   * Add an additional header to the error response which {@link #generateEwpErrorResponse()} will
   * generate.
   *
   * @param key Name of the header.
   * @param value Value for the header.
   */
  protected void putExtraEwpErrorResponseHeader(String key, String value) {
    this.extraResponseHeaders.put(key, value);
  }
}
