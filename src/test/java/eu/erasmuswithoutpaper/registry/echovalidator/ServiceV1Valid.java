package eu.erasmuswithoutpaper.registry.echovalidator;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.List;

import eu.erasmuswithoutpaper.registry.internet.InternetTestHelpers;
import eu.erasmuswithoutpaper.registry.internet.Request;
import eu.erasmuswithoutpaper.registry.internet.Response;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

/**
 * Internal "fake" implementation of a valid Version 1 Echo API endpoint.
 *
 * <p>
 * This implementation is used in tests, to make sure that the validator is working correctly.
 * </p>
 */
public class ServiceV1Valid extends AbstractEchoV1Service {

  public ServiceV1Valid(String url, RegistryClient registryClient) {
    super(url, registryClient);
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
    if (!(request.getMethod().equals("GET") || request.getMethod().equals("POST"))) {
      return this.createErrorResponse(request, 405, "We expect GETs and POSTs only");
    }
    List<String> echos = InternetTestHelpers.extractParams(request, "echo");
    return this.createEchoResponse(request, echos,
        this.registryClient.getHeisCoveredByCertificate(cert));
  }

}
