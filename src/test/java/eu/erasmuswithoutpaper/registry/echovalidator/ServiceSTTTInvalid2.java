package eu.erasmuswithoutpaper.registry.echovalidator;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.List;

import eu.erasmuswithoutpaper.registry.internet.Internet.Request;
import eu.erasmuswithoutpaper.registry.internet.Internet.Response;
import eu.erasmuswithoutpaper.registry.internet.InternetTestHelpers;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

/**
 * Internal "fake" implementation of an invalid STTT Echo API endpoint.
 *
 * <p>
 * Invalid because it returned HTTP 400 in place of HTTP 403 and HTTP 405.
 * </p>
 */
public class ServiceSTTTInvalid2 extends AbstractEchoV2Service {

  public ServiceSTTTInvalid2(String url, RegistryClient registryClient) {
    super(url, registryClient);
  }

  @Override
  public Response handleInternetRequest2(Request request) throws IOException {
    if (!request.getUrl().startsWith(this.myEndpoint)) {
      return null;
    }
    if (!request.getClientCertificate().isPresent()) {
      return this.createErrorResponse(request, 400, "Expecting client certificate.");
    }
    X509Certificate cert = request.getClientCertificate().get();
    if (!this.registryClient.isCertificateKnown(cert)) {
      return this.createErrorResponse(request, 400, "Unknown client certificate.");
    }
    if (!(request.getMethod().equals("GET") || request.getMethod().equals("POST"))) {
      return this.createErrorResponse(request, 400, "We expect GETs and POSTs only");
    }
    List<String> echos = InternetTestHelpers.extractParams(request, "echo");
    return this.createEchoResponse(request, echos,
        this.registryClient.getHeisCoveredByCertificate(cert));
  }

}
