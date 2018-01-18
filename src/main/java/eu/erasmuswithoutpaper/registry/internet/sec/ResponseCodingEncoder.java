package eu.erasmuswithoutpaper.registry.internet.sec;

import eu.erasmuswithoutpaper.registry.internet.Response;

/**
 * An encoder for {@link Response}s.
 *
 * <p>
 * This subclass encodes exactly one coding type, as identified by {@link #getContentEncoding()}. If
 * you want to encode multiple codings at the same time, or your encoding has no assigned
 * Content-Encoding value, then use {@link ResponseEncoder}.
 * </p>
 */
public interface ResponseCodingEncoder extends ResponseEncoder {

  /**
   * @return Lower-case name of the encoding, to be used in Content-Encoding header.
   */
  String getContentEncoding();
}
