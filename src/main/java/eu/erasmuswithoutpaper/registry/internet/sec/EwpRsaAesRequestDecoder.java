package eu.erasmuswithoutpaper.registry.internet.sec;

import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import eu.erasmuswithoutpaper.registry.internet.Request;
import eu.erasmuswithoutpaper.rsaaes.BadEwpRsaAesBody;
import eu.erasmuswithoutpaper.rsaaes.EwpRsaAes128GcmDecoder;
import eu.erasmuswithoutpaper.rsaaes.InvalidRecipient;

import org.apache.commons.codec.digest.DigestUtils;

/**
 * This {@link RequestCodingDecoder} supports <code>ewp-rsa-aes128gcm</code> Content-Encoding.
 */
public class EwpRsaAesRequestDecoder implements RequestCodingDecoder {

  private final List<KeyPair> serverKeys;

  /**
   * @param serverKeys The list {@link KeyPair}s to try decrypting with.
   */
  public EwpRsaAesRequestDecoder(List<KeyPair> serverKeys) {
    this.serverKeys = serverKeys;
  }

  @Override
  public void decode(Request request) throws Http4xx {
    this.updateBody(request);
    this.popContentEncodingAndExpect(request, this.getContentEncoding());
  }

  @Override
  public String getContentEncoding() {
    return "ewp-rsa-aes128gcm";
  }

  /**
   * Choose a proper decryption key for the given encrypted body.
   *
   * @param ewpRsaAesBody Encrypted binary contents.
   * @return Either a {@link KeyPair}, or <code>null</code> if none of our {@link KeyPair}s matched
   *         the recipient's key for which the content was encrypted to.
   * @throws BadEwpRsaAesBody If the encrypted content is broken in some way (it's not in a proper
   *         format).
   */
  protected KeyPair chooseKey(byte[] ewpRsaAesBody) throws BadEwpRsaAesBody {
    byte[] recipientFingerprint =
        EwpRsaAes128GcmDecoder.extractRecipientPublicKeySha256(ewpRsaAesBody);
    for (KeyPair keyPair : this.serverKeys) {
      byte[] keyFingerprint = DigestUtils.sha256(keyPair.getPublic().getEncoded());
      if (Arrays.equals(keyFingerprint, recipientFingerprint)) {
        return keyPair;
      }
    }
    return null;
  }

  /**
   * Get the "outermost" Content-Encoding.
   *
   * @param request The request to peek at.
   * @return The identifier of the "outermost" of the Content-Encodings of this request, or
   *         <code>null</code> if the request has no encodings left.
   */
  protected String peekContentEncoding(Request request) {
    String value = request.getHeader("Content-Encoding");
    if (value == null) {
      return null;
    }
    String[] items = value.split(", *");
    return items[items.length - 1];
  }

  /**
   * Remove the "outermost" Content-Encoding.
   *
   * @param request The request to remove the Content-Encoding from. It MUST have at least one
   *        Content-Encoding left.
   */
  protected void popContentEncoding(Request request) {
    ArrayList<String> items =
        new ArrayList<>(Arrays.asList(request.getHeader("Content-Encoding").split(", *")));
    items.remove(items.size() - 1);
    request.putHeader("Content-Encoding", items.stream().collect(Collectors.joining(", ")));
  }

  /**
   * Take the "outermost" Content-Encoding, verify that it matches what we expect, <b>and remove
   * it</b>.
   *
   * @param request The request to process.
   * @param expectedCoding The value of the coding we expect to find at the outermost
   *        Content-Encoding.
   * @throws Http4xx If the request's outermost coding didn't match what we expect.
   */
  protected void popContentEncodingAndExpect(Request request, String expectedCoding)
      throws Http4xx {
    String actualCoding = this.peekContentEncoding(request);
    if (actualCoding == null) {
      throw new Http4xx(400,
          "Expecting Content-Encoding to be " + expectedCoding + ", but no encoding found.");
    }
    if (!actualCoding.equalsIgnoreCase(expectedCoding)) {
      throw new Http4xx(400, "Expecting Content-Encoding to be " + expectedCoding + ", but "
          + actualCoding + " found instead.");
    }
    this.popContentEncoding(request);
  }

  /**
   * Decrypt the body.
   *
   * @param request The request to process.
   * @throws Http4xx If the body cannot be decrypted.
   */
  protected void updateBody(Request request) throws Http4xx {
    if (!request.getBody().isPresent()) {
      throw new Http4xx(400, "Missing request body. Cannot decode ewp-rsa-aes128gcm.");
    }
    byte[] ewpRsaAesBody = request.getBody().get();
    byte[] body;
    try {
      KeyPair keyPair = this.chooseKey(ewpRsaAesBody);
      if (keyPair == null) {
        throw new Http4xx(400, "We cannot decrypt this request. Unknown recipient key.");
      }
      EwpRsaAes128GcmDecoder decoder = new EwpRsaAes128GcmDecoder(
          (RSAPublicKey) keyPair.getPublic(), (RSAPrivateKey) keyPair.getPrivate());
      body = decoder.decode(ewpRsaAesBody);
    } catch (BadEwpRsaAesBody e) {
      throw new Http4xx(400, "Could not decode the request.");
    } catch (InvalidRecipient e) {
      // Shouldn't happen. We have already checked this.
      throw new RuntimeException(e);
    }
    request.setBody(body);
  }
}
