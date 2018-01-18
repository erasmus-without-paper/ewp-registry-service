package eu.erasmuswithoutpaper.registry.internet.sec;

import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import java.util.List;

import eu.erasmuswithoutpaper.registry.common.Utils;
import eu.erasmuswithoutpaper.registry.internet.Response;
import eu.erasmuswithoutpaper.rsaaes.BadEwpRsaAesBody;
import eu.erasmuswithoutpaper.rsaaes.EwpRsaAes128GcmDecoder;
import eu.erasmuswithoutpaper.rsaaes.InvalidRecipient;

import org.apache.commons.codec.digest.DigestUtils;

/**
 * This decoder handles the ewp-rsa-aes128gcm response encoding.
 */
public class EwpRsaAesResponseDecoder extends CommonResponseDecoder {

  private final List<KeyPair> serverKeys;

  /**
   * @param serverKeys The list {@link KeyPair}s to try decrypting with.
   */
  public EwpRsaAesResponseDecoder(List<KeyPair> serverKeys) {
    this.serverKeys = serverKeys;
  }

  @Override
  public void decode(Response response) throws InvalidResponseError {
    this.updateBody(response);
    this.popContentEncodingAndExpect(response, this.getContentEncoding());
    response.addProcessingNoticeHtml("Successfully stripped the <code>"
        + Utils.escapeHtml(this.getContentEncoding()) + "</code> Content-Encoding.");
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
   * Decrypt the body.
   *
   * @param response The response to process.
   * @throws InvalidResponseError If the body cannot be decrypted.
   */
  protected void updateBody(Response response) throws InvalidResponseError {
    byte[] ewpRsaAesBody = response.getBody();
    byte[] body;
    try {
      KeyPair keyPair = this.chooseKey(ewpRsaAesBody);
      if (keyPair == null) {
        throw new InvalidResponseError("We cannot decrypt this response. Unknown recipient key.");
      }
      EwpRsaAes128GcmDecoder decoder = new EwpRsaAes128GcmDecoder(
          (RSAPublicKey) keyPair.getPublic(), (RSAPrivateKey) keyPair.getPrivate());
      body = decoder.decode(ewpRsaAesBody);
    } catch (BadEwpRsaAesBody e) {
      throw new InvalidResponseError("Could not decode the response. Invalid EwpRsaAesBody.");
    } catch (InvalidRecipient e) {
      // Shouldn't happen. We have already checked this.
      throw new RuntimeException(e);
    }
    response.setBody(body);
  }

}
