package eu.erasmuswithoutpaper.registry.echovalidator;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;

import eu.erasmuswithoutpaper.registry.internet.InternetTestHelpers;
import eu.erasmuswithoutpaper.registry.internet.Request;
import eu.erasmuswithoutpaper.registry.internet.Response;
import eu.erasmuswithoutpaper.registry.internet.sec.EwpRsaAesRequestDecoder;
import eu.erasmuswithoutpaper.registry.internet.sec.Http4xx;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

public class ServiceSTETValid extends AbstractEchoV2Service {

  private final EwpRsaAesRequestDecoder myDecoder;

  public ServiceSTETValid(String url, RegistryClient registryClient, List<KeyPair> serverKeys) {
    super(url, registryClient, serverKeys);
    this.myDecoder = new EwpRsaAesRequestDecoder(this.serverKeys) {
      @Override
      protected void verifyHttpMethod(Request request) throws Http4xx {
        // Check methods

        boolean methodMatched = false;
        for (String method : ServiceSTETValid.this.getAcceptableMethods()) {
          if (request.getMethod().equals(method)) {
            methodMatched = true;
            break;
          }
        }
        if (!methodMatched) {
          throw new Http4xx(405, "This endpoint accepts the following methods: "
              + Arrays.toString(ServiceSTETValid.this.getAcceptableMethods()));
        }
      };
    };
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

    // Decode the body. (Expect it to be encoded in ewp-rsa-aes128gcm.)

    try {
      this.myDecoder.decode(request);
    } catch (Http4xx e) {
      return e.generateEwpErrorResponse();
    }

    String bodyString = new String(request.getBody().get(), StandardCharsets.UTF_8);
    List<String> echos = InternetTestHelpers.extractParamsFromQueryString(bodyString, "echo");
    return this.createEchoResponse(request, echos,
        this.registryClient.getHeisCoveredByCertificate(cert));
  }

  protected String[] getAcceptableMethods() {
    // Valid - accept POSTs only
    return new String[] { "POST" };
  }
}
