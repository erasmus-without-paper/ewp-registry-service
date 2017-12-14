package eu.erasmuswithoutpaper.registry.echovalidator;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import java.util.List;

import eu.erasmuswithoutpaper.registry.internet.Internet.Request;
import eu.erasmuswithoutpaper.registry.internet.Internet.Response;
import eu.erasmuswithoutpaper.registry.internet.InternetTestHelpers;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;
import eu.erasmuswithoutpaper.rsaaes.BadEwpRsaAesBody;
import eu.erasmuswithoutpaper.rsaaes.EwpRsaAes128GcmDecoder;
import eu.erasmuswithoutpaper.rsaaes.InvalidRecipient;

import org.apache.commons.codec.digest.DigestUtils;

public class ServiceSTETValid extends AbstractEchoV2Service {

  public ServiceSTETValid(String url, RegistryClient registryClient, List<KeyPair> serverKeys) {
    super(url, registryClient, serverKeys);
  }

  @Override
  public Response handleInternetRequest2(Request request) throws IOException {
    if (!request.getUrl().startsWith(this.myEndpoint)) {
      return null;
    }
    if (!request.getClientCertificate().isPresent()) {
      return this.createErrorResponse(request, 403, "Expecting client certificate.");
    }
    X509Certificate cert = request.getClientCertificate().get();
    if (!this.registryClient.isCertificateKnown(cert)) {
      return this.createErrorResponse(request, 403, "Unknown client certificate.");
    }

    // Check methods

    boolean methodMatched = false;
    for (String method : this.getAcceptableMethods()) {
      if (request.getMethod().equals(method)) {
        methodMatched = true;
        break;
      }
    }
    if (!methodMatched) {
      return this.createErrorResponse(request, 405, "This endpoint accepts the following methods: "
          + Arrays.toString(this.getAcceptableMethods()));
    }

    // Make sure that a proper Content-Encoding is present

    if (!"ewp-rsa-aes128gcm".equals(request.getHeader("Content-Encoding"))) {
      return this.createErrorResponse(request, 400,
          "This endpoint accepts only the ewp-rsa-aes128gcm Content-Encoding.");
    }

    // Decrypt the body

    if (!request.getBody().isPresent()) {
      return this.createErrorResponse(request, 400, "Missing request body.");
    }
    byte[] ewpRsaAesBody = request.getBody().get();
    byte[] body;
    try {
      KeyPair keyPair = this.chooseKey(ewpRsaAesBody);
      if (keyPair == null) {
        return this.createErrorResponse(request, 400,
            "We cannot decrypt this request. Unknown recipient key.");
      }
      EwpRsaAes128GcmDecoder decoder = new EwpRsaAes128GcmDecoder(
          (RSAPublicKey) keyPair.getPublic(), (RSAPrivateKey) keyPair.getPrivate());
      body = decoder.decode(ewpRsaAesBody);
    } catch (BadEwpRsaAesBody e) {
      return this.createErrorResponse(request, 400, "Could not decode the request.");
    } catch (InvalidRecipient e) {
      // Shouldn't happen. We have already checked this.
      throw new RuntimeException(e);
    }
    String bodyString = new String(body, StandardCharsets.UTF_8);
    List<String> echos = InternetTestHelpers.extractParamsFromQueryString(bodyString, "echo");
    return this.createEchoResponse(request, echos,
        this.registryClient.getHeisCoveredByCertificate(cert));
  }

  private KeyPair chooseKey(byte[] ewpRsaAesBody) throws BadEwpRsaAesBody {
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

  protected String[] getAcceptableMethods() {
    // Valid - accept POSTs only
    return new String[] { "POST" };
  }
}
