package eu.erasmuswithoutpaper.registry.validators.echovalidator;

import java.io.IOException;
import java.util.List;

import eu.erasmuswithoutpaper.registry.internet.InternetTestHelpers;
import eu.erasmuswithoutpaper.registry.internet.Request;
import eu.erasmuswithoutpaper.registry.internet.Response;
import eu.erasmuswithoutpaper.registry.internet.sec.EwpCertificateRequestAuthorizer;
import eu.erasmuswithoutpaper.registry.internet.sec.EwpClientWithCertificate;
import eu.erasmuswithoutpaper.registry.internet.sec.Http4xx;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

/**
 * Internal "fake" implementation of a valid STTT Echo API endpoint.
 *
 * <p>
 * Same as {@link ServiceV1Valid}, but implements Echo API v2.
 * </p>
 */
public class ServiceSTTTValid extends AbstractEchoV2Service {

  protected EwpCertificateRequestAuthorizer myAuthorizer;

  public ServiceSTTTValid(String url, RegistryClient registryClient) {
    super(url, registryClient);
    this.myAuthorizer = new EwpCertificateRequestAuthorizer(this.registryClient);
  }

  @Override
  public Response handleInternetRequest2(Request request)
      throws IOException, ErrorResponseException {
    if (!request.getUrl().startsWith(this.myEndpoint)) {
      return null;
    }
    EwpClientWithCertificate client;
    try {
      client = this.myAuthorizer.authorize(request);
    } catch (Http4xx e) {
      return e.generateEwpErrorResponse();
    }
    if (!(request.getMethod().equals("GET") || request.getMethod().equals("POST"))) {
      return this.createErrorResponse(request, 405, "We expect GETs and POSTs only");
    }
    List<String> echos = InternetTestHelpers.extractParams(request, "echo");
    return this.createEchoResponse(request, echos,
        this.registryClient.getHeisCoveredByCertificate(client.getCertificate()));
  }

}
