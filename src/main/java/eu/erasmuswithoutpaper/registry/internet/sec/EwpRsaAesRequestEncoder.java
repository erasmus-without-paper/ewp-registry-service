package eu.erasmuswithoutpaper.registry.internet.sec;

import java.security.interfaces.RSAPublicKey;

import eu.erasmuswithoutpaper.registry.common.Utils;
import eu.erasmuswithoutpaper.registry.internet.Request;
import eu.erasmuswithoutpaper.rsaaes.EwpRsaAes128GcmEncoder;

/**
 * This {@link RequestCodingEncoder} supports <code>ewp-rsa-aes128gcm</code> Content-Encoding.
 */
public class EwpRsaAesRequestEncoder implements RequestCodingEncoder {

  /**
   * Encoder used for encrypting.
   */
  protected final EwpRsaAes128GcmEncoder encoder;

  /**
   * @param recipientPublicKey Recipient's public key to use for encryption.
   */
  public EwpRsaAesRequestEncoder(RSAPublicKey recipientPublicKey) {
    this.encoder = new EwpRsaAes128GcmEncoder(recipientPublicKey);
  }

  @Override
  public void encode(Request request) {
    this.addContentEncoding(request, this.getContentEncoding());
    this.updateRequestBody(request);
    request.addProcessingNoticeHtml("Successfully applied the <code>"
        + Utils.escapeHtml(this.getContentEncoding()) + "</code> Content-Encoding.");
  }

  @Override
  public String getContentEncoding() {
    return "ewp-rsa-aes128gcm";
  }

  /**
   * Append a new coding to the request's Content-Encoding header.
   *
   * @param request The request to modify.
   * @param coding The coding identifier to append.
   */
  protected void addContentEncoding(Request request, String coding) {
    String value = request.getHeader("Content-Encoding");
    if (value != null) {
      value += ", ";
    } else {
      value = "";
    }
    value += coding;
    request.putHeader("Content-Encoding", value);
  }

  /**
   * Encrypt the request's body.
   *
   * @param request The request to encrypt.
   */
  protected void updateRequestBody(Request request) {
    byte[] input;
    if (request.getBody().isPresent()) {
      input = request.getBody().get();
    } else {
      input = new byte[0];
    }
    request.setBody(this.encoder.encode(input));
  }
}
