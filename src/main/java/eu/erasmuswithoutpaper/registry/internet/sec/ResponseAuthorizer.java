package eu.erasmuswithoutpaper.registry.internet.sec;

import eu.erasmuswithoutpaper.registry.internet.Request;
import eu.erasmuswithoutpaper.registry.internet.Response;

/**
 * Authorizes {@link Response}s received from Internet.
 *
 * <p>
 * This interface says nothing about the type of authorization used. In particular, it is possible
 * for a single {@link Response} to be properly authorized by one {@link ResponseAuthorizer}, while
 * not being authorized by others.
 * </p>
 */
public interface ResponseAuthorizer {

  /**
   * Authorize a given {@link Response}. This operation MAY change the response's headers and/or
   * body.
   *
   * <p>
   * Identify the server which has made the response, clean up all response contents (i.e. headers)
   * which might have been added "in transport" by untrusted proxies.
   * </p>
   *
   * @param request The {@link Request} for which this response has been received.
   * @param response The {@link Response} to be authorized.
   * @return {@link EwpServer} of the server which has signed the response (depending on the
   *         particular implementation, this may be a very specific {@link EwpServer} subclass).
   * @throws CouldNotAuthorize When the response cannot be properly authorized (e.g. the server's
   *         signature doesn't match what we expect).
   */
  EwpServer authorize(Request request, Response response) throws CouldNotAuthorize;
}
