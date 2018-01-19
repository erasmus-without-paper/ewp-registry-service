package eu.erasmuswithoutpaper.registry.internet.sec;

import eu.erasmuswithoutpaper.registry.internet.Response;

/**
 * A helper class. It includes a couple of methods which are common for all types of
 * {@link ResponseCodingEncoder}s.
 */
public abstract class CommonResponseEncoder implements ResponseCodingEncoder {

  /**
   * Append a new coding to the Content-Encoding header.
   *
   * @param response The response to process.
   * @param coding The coding name to append.
   */
  protected void appendContentEncoding(Response response, String coding) {
    String value = response.getHeader("Content-Encoding");
    if (value != null) {
      value += ", ";
    } else {
      value = "";
    }
    value += coding;
    response.putHeader("Content-Encoding", value);
  }

}
