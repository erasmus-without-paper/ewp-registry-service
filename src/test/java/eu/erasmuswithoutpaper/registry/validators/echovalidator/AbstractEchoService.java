package eu.erasmuswithoutpaper.registry.validators.echovalidator;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;

import eu.erasmuswithoutpaper.registry.common.Utils;
import eu.erasmuswithoutpaper.registry.documentbuilder.KnownNamespace;
import eu.erasmuswithoutpaper.registry.internet.FakeInternetService;
import eu.erasmuswithoutpaper.registry.internet.Request;
import eu.erasmuswithoutpaper.registry.internet.Response;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

abstract public class AbstractEchoService implements FakeInternetService {

  /**
   * Helper class for easier delegation of error responses in {@link AbstractEchoService}.
   */
  protected static class ErrorResponseException extends Exception {
    private static final long serialVersionUID = 4917758848531475570L;

    protected final Response response;

    public ErrorResponseException(Response response) {
      this.response = response;
    }
  }

  protected final String myEndpoint;
  protected final RegistryClient registryClient;

  /**
   * @param url The endpoint at which to listen for requests.
   * @param registryClient Initialized and refreshed {@link RegistryClient} instance.
   */
  public AbstractEchoService(String url, RegistryClient registryClient) {
    this.myEndpoint = url;
    this.registryClient = registryClient;
  }

  @Override
  public Response handleInternetRequest(Request request) throws IOException {
    try {
      return this.handleInternetRequest2(request);
    } catch (ErrorResponseException e) {
      return e.response;
    }
  }

  protected Response createEchoResponse(Request request, String xmlns, List<String> echos,
      Collection<String> heiIds) {
    StringBuilder sb = new StringBuilder();
    sb.append("<response xmlns='").append(xmlns).append("'>");
    for (String heiId : heiIds) {
      sb.append("<hei-id>").append(Utils.escapeXml(heiId)).append("</hei-id>");
    }
    for (String echo : echos) {
      sb.append("<echo>").append(Utils.escapeXml(echo)).append("</echo>");
    }
    sb.append("</response>");
    return new Response(200, sb.toString().getBytes(StandardCharsets.UTF_8));
  }

  protected Response createErrorResponse(Request request, int status, String developerMessage) {
    StringBuilder sb = new StringBuilder();
    String NS = KnownNamespace.COMMON_TYPES_V1.getNamespaceUri();
    sb.append("<error-response xmlns='").append(NS).append("'>");
    sb.append("<developer-message>");
    sb.append(Utils.escapeXml(developerMessage));
    sb.append("</developer-message>");
    sb.append("</error-response>");
    return new Response(status, sb.toString().getBytes(StandardCharsets.UTF_8));
  }

  /**
   * @param request The request for which a response is to be generated
   * @return Either <code>null</code> or {@link Response} object. If this service doesn't cover this
   *         particular request (for example the request is for a different domain), then
   *         <code>null</code> should be returned.
   * @throws IOException
   * @throws ErrorResponseException This can be thrown instead of returning the error response (a
   *         shortcut).
   */
  abstract protected Response handleInternetRequest2(Request request)
      throws IOException, ErrorResponseException;
}
