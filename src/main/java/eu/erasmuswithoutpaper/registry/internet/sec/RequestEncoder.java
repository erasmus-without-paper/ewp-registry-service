package eu.erasmuswithoutpaper.registry.internet.sec;

import eu.erasmuswithoutpaper.registry.internet.Request;

/**
 * An encoder for {@link Request}s.
 *
 * <p>
 * This interface says nothing about the type of encoding. It may join multiple encodings, and it
 * doesn't have to rely on the Content-Encoding header (but it may).
 * </p>
 */
public interface RequestEncoder {

  /**
   * Encode a given {@link Request}. This operation MAY change the request headers and/or body.
   *
   * @param request The {@link Request} instance to process.
   */
  void encode(Request request);
}
