package eu.erasmuswithoutpaper.registry.internet.sec;

import eu.erasmuswithoutpaper.registry.internet.Request;

/**
 * A decoder for {@link Request}s.
 *
 * <p>
 * This interface says nothing about the type of encoding.
 * </p>
 */
public interface RequestDecoder {

  /**
   * Decode a given {@link Request}. This operation MAY change the request headers and/or body.
   *
   * @param request The {@link Request} instance to process.
   * @throws Http4xx If the content is not encoded correctly.
   */
  void decode(Request request) throws Http4xx;
}
