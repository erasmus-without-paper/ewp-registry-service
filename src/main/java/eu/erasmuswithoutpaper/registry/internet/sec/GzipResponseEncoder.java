package eu.erasmuswithoutpaper.registry.internet.sec;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;

import eu.erasmuswithoutpaper.registry.common.Utils;
import eu.erasmuswithoutpaper.registry.internet.Request;
import eu.erasmuswithoutpaper.registry.internet.Response;

/**
 * Encodes responses with gzip Content-Encoding.
 */
public class GzipResponseEncoder extends CommonResponseEncoder {

  @Override
  public void encode(Request request, Response response) throws Http4xx {
    this.encode(response);
  }

  /**
   * This encoder doesn't need {@link Request} object to work properly. You may call this method
   * instead {@link #encode(Request, Response)}, the result will be the same.
   *
   * @param response The response to apply gzip on.
   */
  public void encode(Response response) {
    this.updateResponseBody(response);
    this.appendContentEncoding(response, this.getContentEncoding());
    response.addProcessingNoticeHtml("Successfully applied the <code>"
        + Utils.escapeHtml(this.getContentEncoding()) + "</code> Content-Encoding.");
  }

  @Override
  public String getContentEncoding() {
    return "gzip";
  }

  @Override
  public String toString() {
    return "Gzip Response Encoder";
  }

  /**
   * Gzip response body.
   *
   * @param response The response to modify.
   */
  protected void updateResponseBody(Response response) {
    try {
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      GZIPOutputStream gzip = new GZIPOutputStream(output);
      gzip.write(response.getBody());
      gzip.close();
      response.setBody(output.toByteArray());
    } catch (IOException e) {
      throw new RuntimeException();
    }
  }
}
