package eu.erasmuswithoutpaper.registry.internet.sec;

import java.net.MalformedURLException;
import java.net.URL;

import eu.erasmuswithoutpaper.registry.internet.Request;
import eu.erasmuswithoutpaper.registry.internet.Response;

/**
 * A helper class. It includes a couple of verification methods which are common for all types of
 * {@link ResponseAuthorizer}s.
 */
public abstract class CommonResponseAuthorizer implements ResponseAuthorizer {

  /**
   * Return the URL at which the request was made.
   *
   * @param request The request.
   * @return The URL at which the request was made.
   * @throws InvalidResponseError When the URL could not be parsed.
   */
  protected URL parseUrl(Request request) throws InvalidResponseError {
    URL url;
    try {
      url = new URL(request.getUrl());
    } catch (MalformedURLException e) {
      throw new InvalidResponseError("Malformed request URL - cannot authorize the response.");
    }
    return url;
  }

  /**
   * Verify that a proper, safe protocol is used.
   *
   * @param url The URL to verify.
   * @throws InvalidResponseError If an unsafe protocol is used.
   */
  protected void verifyProtocol(URL url) throws InvalidResponseError {
    if (!url.getProtocol().equalsIgnoreCase("https")) {
      throw new InvalidResponseError("Requests need to be made over TLS (https) connection.");
    }
  }

  /**
   * Verify if response's X-Request-Id header is correct (i.e. if it matches the value sent in the
   * request).
   *
   * @param request The request for which the response has been generated.
   * @param response The response to verify.
   * @throws InvalidResponseError If something was wrong with the response's X-Request-Id header.
   */
  protected void verifyRequestId(Request request, Response response) throws InvalidResponseError {
    String reqReqId = request.getHeader("X-Request-Id");
    String resReqId = response.getHeader("X-Request-Id");
    if (resReqId != null) {
      // If present in response, then it should also be present in the request.
      if (reqReqId == null) {
        throw new InvalidResponseError("The request didn't contain the X-Request-Id header, so "
            + "the response also shouldn't.");
      } else {
        // Both should be equal.
        if (!reqReqId.equals(resReqId)) {
          throw new InvalidResponseError(
              "Expecting the response to contain exactly the same X-Request-Id "
                  + "as has been sent in the request.");
        }
      }
    } else {
      // Not present in the response. This might be a failure, but this depends on a particular
      // response authorization scheme (and should be validated in subclasses).
    }
  }
}
