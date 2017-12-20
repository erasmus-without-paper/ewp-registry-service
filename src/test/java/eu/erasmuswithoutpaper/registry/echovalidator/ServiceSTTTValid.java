package eu.erasmuswithoutpaper.registry.echovalidator;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.List;

import eu.erasmuswithoutpaper.registry.internet.Internet.Request;
import eu.erasmuswithoutpaper.registry.internet.Internet.Response;
import eu.erasmuswithoutpaper.registry.internet.InternetTestHelpers;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

/**
 * Internal "fake" implementation of a valid STTT Echo API endpoint.
 *
 * <p>
 * Same as {@link ServiceV1Valid}, but implements Echo API v2.
 * </p>
 */
public class ServiceSTTTValid extends AbstractEchoV2Service {

  public ServiceSTTTValid(String url, RegistryClient registryClient) {
    super(url, registryClient);
  }

  @Override
  public Response handleInternetRequest2(Request request)
      throws IOException, ErrorResponseException {
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
