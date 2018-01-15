package eu.erasmuswithoutpaper.registry.internet.sec;

import eu.erasmuswithoutpaper.registry.internet.Request;
import eu.erasmuswithoutpaper.registry.internet.Response;

/**
 * A helper class. It includes a couple of signing methods which are common for all types of
 * {@link ResponseSigner}s.
 */
public abstract class CommonResponseSigner implements ResponseSigner {

  /**
   * Fill the response with request's X-Request-Id header (if it is present in the request).
   *
   * @param request The request to take the header value from.
   * @param response The response to process.
   */
  protected void includeXRequestIdHeader(Request request, Response response) {
    String reqId = request.getHeader("X-Request-Id");
    if (reqId != null) {
      response.putHeader("X-Request-Id", reqId);
    }
  }
}
