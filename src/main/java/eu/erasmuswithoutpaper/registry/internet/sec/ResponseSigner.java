package eu.erasmuswithoutpaper.registry.internet.sec;

import eu.erasmuswithoutpaper.registry.internet.Request;
import eu.erasmuswithoutpaper.registry.internet.Response;

/**
 * A signer for {@link Response}s.
 *
 * <p>
 * This interface says nothing about the type of signature.
 * </p>
 */
public interface ResponseSigner {

  /**
   * Sign a given {@link Response}. This operation MAY change the response headers and/or body, but
   * it should not change the request.
   *
   * @param request The {@link Request} instance for which the response has been generated. (It is
   *        provided here, because it will often contain additional details on how the response
   *        should be signed.)
   * @param response The {@link Response} instance to process.
   * @throws Http4xx If the signing properties provided by the caller in the request are missing or
   *         invalid.
   */
  void sign(Request request, Response response) throws Http4xx;
}
