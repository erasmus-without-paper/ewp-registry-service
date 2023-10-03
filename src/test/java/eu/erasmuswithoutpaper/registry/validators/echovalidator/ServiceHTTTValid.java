package eu.erasmuswithoutpaper.registry.validators.echovalidator;

import java.io.IOException;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import eu.erasmuswithoutpaper.registry.internet.Request;
import eu.erasmuswithoutpaper.registry.internet.Response;
import eu.erasmuswithoutpaper.registry.internet.sec.EwpClientWithRsaKey;
import eu.erasmuswithoutpaper.registry.internet.sec.EwpHttpSigRequestAuthorizer;
import eu.erasmuswithoutpaper.registry.internet.sec.Http4xx;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

/**
 * Internal "fake" implementation of a valid HTTT API endpoint.
 */
public class ServiceHTTTValid extends AbstractEchoV2Service {

  private final EwpHttpSigRequestAuthorizer myAuthorizer;

  public ServiceHTTTValid(String url, RegistryClient registryClient) {
    super(url, registryClient);
    this.myAuthorizer = this.newAuthorizer();
  }

  @Override
  public Response handleInternetRequest2(Request request)
      throws IOException, ErrorResponseException {

    if (!request.getUrl().startsWith(this.myEndpoint)) {
      return null;
    }
    this.verifyHttpMethod(request);
    EwpClientWithRsaKey client;
    try {
      client = this.myAuthorizer.authorize(request);
    } catch (Http4xx e) {
      return e.generateEwpErrorResponse();
    }

    return this.createEchoResponse(request, this.retrieveEchoValues(request),
        this.identifyCoveredHeis(client.getRsaPublicKey()));
  }

  protected List<String> getAcceptedHttpMethods() {
    return Arrays.asList("GET", "POST");
  }

  protected Collection<String> identifyCoveredHeis(RSAPublicKey clientKey) {
    return this.registryClient.getHeisCoveredByClientKey(clientKey);
  }

  protected EwpHttpSigRequestAuthorizer newAuthorizer() {
    return new EwpHttpSigRequestAuthorizer(this.registryClient);
  }

  protected void verifyHttpMethod(Request request) throws ErrorResponseException {
    if (!this.getAcceptedHttpMethods().contains(request.getMethod())) {
      throw new ErrorResponseException(this.createErrorResponse(request, 405,
          "Accepted HTTP methods: " + this.getAcceptedHttpMethods()));
    }
  }
}
