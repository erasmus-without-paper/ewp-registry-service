package eu.erasmuswithoutpaper.registry.echovalidator;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;

import eu.erasmuswithoutpaper.registry.internet.Internet.Request;
import eu.erasmuswithoutpaper.registry.internet.Internet.Response;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;
import eu.erasmuswithoutpaper.rsaaes.EwpRsaAes128GcmEncoder;

import net.adamcin.httpsig.api.Authorization;

public class ServiceSTTEValid extends ServiceSTTTValid {

  public static class InvalidKeyHeader extends Exception {
  }

  public ServiceSTTEValid(String url, RegistryClient registryClient) {
    super(url, registryClient);
  }

  @Override
  public Response handleInternetRequest2(Request request)
      throws IOException, ErrorResponseException {
    Response response = super.handleInternetRequest2(request);

    // Make sure that the client wants us to encrypt the response.

    boolean encryptionRequested = this.wasEwpEncryptionRequested(request);
    if (!encryptionRequested) {
      return this.createErrorResponse(request, 406,
          "This endpoint requires the client to support response encryption.");
    }

    // Declare all codings

    List<String> codings = this.getCodingsToApply(request);
    response.putHeader("Content-Encoding", codings.stream().collect(Collectors.joining(", ")));

    // Apply all codings

    byte[] body = response.getBodyRaw();
    for (String coding : codings) {
      body = this.applyCoding(request, body, coding);
    }
    response.setBody(body);

    return response;
  }

  private boolean wasEwpEncryptionRequested(Request request) {
    for (String coding : request.getAcceptableCodings()) {
      if (coding.equalsIgnoreCase("ewp-rsa-aes128gcm")) {
        return true;
      }
    }
    return false;
  }

  protected byte[] applyCoding(Request request, byte[] input, String coding)
      throws ErrorResponseException {
    if (coding.equalsIgnoreCase("gzip")) {
      return this.applyGzip(input);
    } else if (coding.equalsIgnoreCase("ewp-rsa-aes128gcm")) {
      return this.applyEwpEncryption(request, input);
    } else {
      // coding is provided by us, so this shouldn't happen.
      throw new RuntimeException("Unsupported coding: " + coding);
    }
  }

  protected byte[] applyEwpEncryption(Request request, byte[] input) throws ErrorResponseException {
    // Check for encryption key

    RSAPublicKey recipientKey;
    try {
      recipientKey = this.determineRecipientKey(request);
    } catch (InvalidKeyHeader e) {
      throw new ErrorResponseException(
          this.createErrorResponse(request, 400, "The Accept-Response-Encryption-Key header, "
              + "when present, must contain a valid Base64-encoded RSA public key."));
    }
    if (recipientKey == null) {
      throw new ErrorResponseException(this.createErrorResponse(request, 406,
          "Could not determine the recipient's RSA Public Key "
              + "needed to encrypt the response."));
    }
    EwpRsaAes128GcmEncoder encoder = new EwpRsaAes128GcmEncoder(recipientKey);
    return encoder.encode(input);
  }

  protected byte[] applyGzip(byte[] input) {
    try {
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      GZIPOutputStream gzip = new GZIPOutputStream(output);
      gzip.write(input);
      gzip.close();
      return output.toByteArray();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  protected RSAPublicKey determineRecipientKey(Request request) throws InvalidKeyHeader {
    String keyBase64 = request.getHeader("Accept-Response-Encryption-Key");
    if (keyBase64 != null) {
      return this.extractRecipientKeyFromBase64(keyBase64);
    } else {
      return this.guessRecipientKeyFromOtherHeaders(request);
    }
  }

  protected RSAPublicKey extractRecipientKeyFromBase64(String keyBase64) throws InvalidKeyHeader {
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

  protected List<String> getCodingsToApply(Request request) {
    List<String> codings = new ArrayList<>();
    boolean acceptGzip = false;
    for (String coding : request.getAcceptableCodings()) {
      if (coding.equalsIgnoreCase("gzip")) {
        acceptGzip = true;
      }
    }
    if (acceptGzip) {
      codings.add("gzip");
    }
    codings.add("ewp-rsa-aes128gcm");
    return codings;
  }

  protected RSAPublicKey guessRecipientKeyFromHttpsigHeaders(Request request) {
    Authorization authz = Authorization.parse(request.getHeader("Authorization"));
    if (authz == null) {
      return null;
    }
    String keyId = authz.getKeyId();
    return this.registryClient.findRsaPublicKey(keyId);
  }

  protected RSAPublicKey guessRecipientKeyFromOtherHeaders(Request request) {
    return this.guessRecipientKeyFromHttpsigHeaders(request);
  }
}
