package eu.erasmuswithoutpaper.registry.internet.sec;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Optional;

import eu.erasmuswithoutpaper.registry.common.Utils;
import eu.erasmuswithoutpaper.registry.internet.Request;
import eu.erasmuswithoutpaper.registry.internet.Response;
import eu.erasmuswithoutpaper.rsaaes.EwpRsaAes128GcmEncoder;

/**
 * This encoder handles the ewp-rsa-aes128gcm response encoding.
 */
public class EwpRsaAesResponseEncoder extends CommonResponseEncoder {

  /**
   * Thrown when the key received in the Accept-Response-Encryption-Key header is invalid.
   */
  @SuppressWarnings("serial")
  protected static class InvalidKeyHeader extends Exception {
  }

  /**
   * The default key to use when no other key is found in the Accept-Response-Encryption-Key header.
   */
  protected final Optional<RSAPublicKey> defaultKey;

  /**
   * The message to include in the thrown {@link Http4xx} exceptions, in case when
   * Accept-Response-Encryption-Key header is missing. This message is provided whenever
   * {@link #defaultKey} is not.
   */
  protected final Optional<String> whereDidWeLook;

  /**
   * Initialize the encoder with a default key.
   *
   * @param defaultKey The default key to use for encryption. This key MAY still get overridden by
   *        the Accept-Response-Encryption-Key header.
   */
  public EwpRsaAesResponseEncoder(RSAPublicKey defaultKey) {
    this.defaultKey = Optional.of(defaultKey);
    this.whereDidWeLook = Optional.empty();
  }

  /**
   * Initialize the encoder without a default key. (In this case, the request will need to contain
   * the Accept-Response-Encryption-Key header.)
   *
   * @param whereDidWeLook The message explaining where did the caller look for the default key, why
   *        couldn't he find it, etc. This message will be included in {@link Http4xx} errors thrown
   *        by {@link #encode(Request, Response)}, if the request doesn't contain a proper
   *        Accept-Response-Encryption-Key header.
   */
  public EwpRsaAesResponseEncoder(String whereDidWeLook) {
    this.defaultKey = Optional.empty();
    this.whereDidWeLook = Optional.of(whereDidWeLook);
  }

  @Override
  public void encode(Request request, Response response) throws Http4xx {
    this.updateResponseBody(request, response);
    this.appendContentEncoding(response, this.getContentEncoding());
    response.addProcessingNoticeHtml("Successfully applied the <code>"
        + Utils.escapeHtml(this.getContentEncoding()) + "</code> Content-Encoding.");
  }

  @Override
  public String getContentEncoding() {
    return "ewp-rsa-aes128gcm";
  }

  /**
   * Decode {@link RSAPublicKey} from given base64-encoded value.
   *
   * @param keyBase64 Base64-encoded {@link X509EncodedKeySpec}.
   * @return The decoded key.
   * @throws InvalidKeyHeader If the key is invalid.
   */
  protected RSAPublicKey decodeBase64RsaPublicKey(String keyBase64) throws InvalidKeyHeader {
    try {
      byte[] keyEncoded = Base64.getMimeDecoder().decode(keyBase64);
      X509EncodedKeySpec spec = new X509EncodedKeySpec(keyEncoded);
      KeyFactory kf = KeyFactory.getInstance("RSA");
      return (RSAPublicKey) kf.generatePublic(spec);
    } catch (IllegalArgumentException | InvalidKeySpecException e) {
      throw new InvalidKeyHeader();
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Extract {@link RSAPublicKey} from request's Accept-Response-Encryption-Key header.
   *
   * @param request The request to extract from.
   * @return The extracted key, or empty optional if no header is present.
   * @throws Http4xx If the header is present, but it doesn't contain a valid key.
   */
  protected Optional<RSAPublicKey> extractKeyFromAcceptHeader(Request request) throws Http4xx {
    String keyBase64 = request.getHeader("Accept-Response-Encryption-Key");
    if (keyBase64 == null) {
      return Optional.empty();
    }
    try {
      return Optional.of(this.decodeBase64RsaPublicKey(keyBase64));
    } catch (InvalidKeyHeader e) {
      throw new Http4xx(400, "The Accept-Response-Encryption-Key header, "
          + "when present, must contain a valid Base64-encoded RSA public key.");
    }
  }

  /**
   * Get a proper {@link EwpRsaAes128GcmEncoder} for the given request.
   *
   * @param request The request to extract encoder-related information from.
   * @return The encoder to use for response.
   * @throws Http4xx If the request didn't contain enough information to construct a proper encoder.
   */
  protected EwpRsaAes128GcmEncoder getEncoderForRequest(Request request) throws Http4xx {
    Optional<RSAPublicKey> keyFromHeader = this.extractKeyFromAcceptHeader(request);
    if (keyFromHeader.isPresent()) {
      return new EwpRsaAes128GcmEncoder(keyFromHeader.get());
    } else if (this.defaultKey.isPresent()) {
      return new EwpRsaAes128GcmEncoder(this.defaultKey.get());
    } else {
      throw new Http4xx(400,
          "Could not determine the target encryption key. "
              + "The Accept-Response-Encryption-Key header was not provided, and the "
              + "default key could not be determined. " + this.whereDidWeLook.get());
    }
  }

  /**
   * Encrypt response body.
   *
   * @param request The request for which this response was generated for.
   * @param response The response to encrypt.
   * @throws Http4xx If the request didn't contain enough information to construct a proper encoder.
   */
  protected void updateResponseBody(Request request, Response response) throws Http4xx {
    response.setBody(this.getEncoderForRequest(request).encode(response.getBody()));
  }

}
