package eu.erasmuswithoutpaper.registry.internet.sec;

import eu.erasmuswithoutpaper.registry.internet.Request;

/**
 * A signer for {@link Request}s.
 *
 * <p>
 * This interface says nothing about the type of signature.
 * </p>
 */
public interface RequestSigner {

  /**
   * Sign a given {@link Request}. This operation MAY change the request headers and/or body.
   *
   * @param request The {@link Request} instance to process.
   */
  void sign(Request request);
}
