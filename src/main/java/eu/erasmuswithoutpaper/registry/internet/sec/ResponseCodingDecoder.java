package eu.erasmuswithoutpaper.registry.internet.sec;

import eu.erasmuswithoutpaper.registry.internet.Response;

/**
 * A decoder for {@link Response}s.
 *
 * <p>
 * This subclass decodes exactly one coding type, as identified by {@link #getContentEncoding()}.
 * </p>
 */
public interface ResponseCodingDecoder extends ResponseDecoder {

  /**
   * @return Lower-case name of the encoding, to be used in Content-Encoding header.
   */
  String getContentEncoding();
}
