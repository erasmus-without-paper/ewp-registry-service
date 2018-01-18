package eu.erasmuswithoutpaper.registry.internet.sec;

import eu.erasmuswithoutpaper.registry.internet.Response;

/**
 * A decoder for {@link Response}s.
 *
 * <p>
 * This interface says nothing about the type of encoding.
 * </p>
 */
public interface ResponseDecoder {

  /**
   * Decode a given {@link Response}. This operation MAY change the response's headers and/or body.
   *
   * @param response The {@link Response} instance to process.
   * @throws InvalidResponseError If the content is not encoded correctly.
   */
  void decode(Response response) throws InvalidResponseError;
}
