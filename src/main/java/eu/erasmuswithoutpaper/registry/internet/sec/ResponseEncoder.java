package eu.erasmuswithoutpaper.registry.internet.sec;

import eu.erasmuswithoutpaper.registry.internet.Request;
import eu.erasmuswithoutpaper.registry.internet.Response;

/**
 * An encoder for {@link Response}s.
 *
 * <p>
 * This interface says nothing about the type of encoding. It may join multiple encodings, and it
 * doesn't have to rely on the Content-Encoding header (but it may).
 * </p>
 */
public interface ResponseEncoder {

  /**
   * Encode a given {@link Response}. This operation MAY change the response's headers and/or body.
   *
   * @param request The {@link Request} for which this response was generated.
   * @param response The {@link Response} instance to process.
   * @throws Http4xx If this encoding is dependent on some conditions present in the request, and
   *         these conditions were not met (e.g. missing headers).
   */
  void encode(Request request, Response response) throws Http4xx;
}
