package eu.erasmuswithoutpaper.registry.internet.sec;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;

import eu.erasmuswithoutpaper.registry.common.Utils;
import eu.erasmuswithoutpaper.registry.internet.Response;

import org.apache.commons.io.IOUtils;

/**
 * GZIP coding response decoder.
 */
public class GzipResponseDecoder extends CommonResponseDecoder {

  @Override
  public void decode(Response response) throws InvalidResponseError {
    this.updateBody(response);
    this.popContentEncodingAndExpect(response, this.getContentEncoding());
    response.addProcessingNoticeHtml("Successfully stripped the <code>"
        + Utils.escapeHtml(this.getContentEncoding()) + "</code> Content-Encoding.");
  }

  @Override
  public String getContentEncoding() {
    return "gzip";
  }

  /**
   * Decompress given response's body.
   *
   * @param response The response to process.
   */
  protected void updateBody(Response response) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try {
      IOUtils.copy(new GZIPInputStream(new ByteArrayInputStream(response.getBody())), out);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    response.setBody(out.toByteArray());
  }

}
