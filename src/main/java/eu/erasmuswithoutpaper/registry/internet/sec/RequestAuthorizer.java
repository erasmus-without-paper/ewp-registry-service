package eu.erasmuswithoutpaper.registry.internet.sec;

import eu.erasmuswithoutpaper.registry.internet.Request;

/**
 * Authorizes {@link Request}s received from Internet.
 *
 * <p>
 * This interface says nothing about the type of authorization used. In particular, it is possible
 * for a single {@link Request} to be properly authorized by one {@link RequestAuthorizer}, while
 * not being authorized by others. Some {@link RequestAuthorizer}s may also simply authorize all
 * requests (e.g. when accessing a publicly available resource).
 * </p>
 */
public interface RequestAuthorizer {

  /**
   * Authorize a given {@link Request}. This operation MAY change the request's headers and/or body.
   *
   * <p>
   * Identify the client which has made the request, clean up all request contents (i.e. headers)
   * which might have been added "in transport" by untrusted proxies.
   * </p>
   *
   * @param request The {@link Request} instance to process.
   * @return {@link ClientInfo} of the client which has made the request (depending on the
   *         particular implementation, this may be a very specific {@link ClientInfo} subclass, it
   *         also MAY be an anonymous client.
   * @throws Http4xx If the request was not properly authorized - either the client didn't try to
   *         authenticate himself with the expected authentication method, or he tried, but didn't
   *         authenticate himself properly.
   */
  ClientInfo authorize(Request request) throws Http4xx;
}
