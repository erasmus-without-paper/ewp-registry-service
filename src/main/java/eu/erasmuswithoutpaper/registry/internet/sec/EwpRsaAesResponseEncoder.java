package eu.erasmuswithoutpaper.registry.internet.sec;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import eu.erasmuswithoutpaper.registry.common.Utils;
import eu.erasmuswithoutpaper.registry.internet.Request;
import eu.erasmuswithoutpaper.registry.internet.Response;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;
import eu.erasmuswithoutpaper.rsaaes.EwpRsaAes128GcmEncoder;

import com.google.common.collect.Lists;
import net.adamcin.httpsig.api.Authorization;

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

  private final RegistryClient registryClient;

  /**
   * @param registryClient Needed to fetch the actual public key from Authorization header's keyId
   *        (in case when the request is using HTTP Signatures for authentication, it doesn't need
   *        to provide the explicit key in Accept-Response-Encryption-Key header).
   */
  public EwpRsaAesResponseEncoder(RegistryClient registryClient) {
    this.registryClient = registryClient;
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

  @Override
  public String toString() {
    return "ewp-rsa-aes128gcm Response Encoder";
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
   * Try to extract the encryption key from the HTTP Signature's Authorization header.
   *
   * @param request The request to extract from.
   * @return The {@link RSAPublicKey}, if present in the Authorization header.
   * @throws Http4xx If the authorization header was present, but its keyId identified an unknown
   *         key. This is probably unintended client error.
   */
  protected Optional<RSAPublicKey> extractKeyFromAuthorizationHeader(Request request)
      throws Http4xx {
    Authorization authz = Authorization.parse(request.getHeader("Authorization"));
    if (authz == null) {
      return Optional.empty();
    }
    String keyId = authz.getKeyId();
    RSAPublicKey key = this.registryClient.findRsaPublicKey(keyId);
    if (key == null) {
      throw new Http4xx(400,
          "Could not find the key body for keyId used in the Authorization header "
              + "(our registry client doesn't recognize it). "
              + "We cannot use it for encryption.");
    }
    return Optional.of(key);
  }

  /**
   * Determine the best encryption key for the request.
   *
   * @param request The request to process.
   * @return The best encryption key to use, or an empty optional if no encryption key could have
   *         been determined.
   * @throws Http4xx If there was something wrong with the request, and the caller should be
   *         notified about it (e.g. the caller tried to attach the encryption key, but has done so
   *         in invalid way).
   */
  protected Optional<RSAPublicKey> extractKeyFromRequest(Request request) throws Http4xx {
    Optional<RSAPublicKey> keyFromAccept = this.extractKeyFromAcceptHeader(request);
    if (keyFromAccept.isPresent()) {
      return keyFromAccept;
    }
    Optional<RSAPublicKey> keyFromAuth = this.extractKeyFromAuthorizationHeader(request);
    if (keyFromAuth.isPresent()) {
      return keyFromAuth;
    }
    return Optional.empty();
  }

  /**
   * Get a proper {@link EwpRsaAes128GcmEncoder} for the given request.
   *
   * @param request The request to extract encoder-related information from.
   * @return The encoder to use for response.
   * @throws Http4xx If the request didn't contain enough information to construct a proper encoder.
   */
  protected EwpRsaAes128GcmEncoder getEncoderForRequest(Request request) throws Http4xx {
    Optional<RSAPublicKey> encryptionKey = this.extractKeyFromRequest(request);
    if (encryptionKey.isPresent()) {
      return new EwpRsaAes128GcmEncoder(encryptionKey.get());
    }
    throw new Http4xx(400,
        "Could not determine the target encryption key. "
            + "We have looked in the following places: "
            + this.getKeyExtractionSources().stream().collect(Collectors.joining(", ")) + ".");
  }

  /**
   * @return That's simply a list of messages stating where has the
   *         {@link #extractKeyFromRequest(Request)} searched the keys for.
   */
  protected List<String> getKeyExtractionSources() {
    return Lists.newArrayList("Accept-Response-Encryption-Key header",
        "HTTPSIG's Authorization header");
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
