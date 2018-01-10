package eu.erasmuswithoutpaper.registry.internet.sec;

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
   * Sign a given {@link Response}. This operation MAY change the response headers and/or body.
   *
   * @param response The {@link Response} instance to process.
   */
  void sign(Response response);
}
