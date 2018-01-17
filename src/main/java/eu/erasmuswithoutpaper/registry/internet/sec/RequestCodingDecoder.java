package eu.erasmuswithoutpaper.registry.internet.sec;

import eu.erasmuswithoutpaper.registry.internet.Request;

/**
 * An encoder for {@link Request}s.
 *
 * <p>
 * This subclass decodes exactly one coding type, as identified by {@link #getContentEncoding()}.
 * </p>
 */
public interface RequestCodingDecoder extends RequestDecoder {

  /**
   * @return Lower-case name of the encoding, to be used in Content-Encoding header.
   */
  String getContentEncoding();
}
