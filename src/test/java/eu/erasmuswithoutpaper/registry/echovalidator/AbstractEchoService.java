package eu.erasmuswithoutpaper.registry.echovalidator;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;

import eu.erasmuswithoutpaper.registry.documentbuilder.KnownNamespace;
import eu.erasmuswithoutpaper.registry.internet.FakeInternetService;
import eu.erasmuswithoutpaper.registry.internet.Request;
import eu.erasmuswithoutpaper.registry.internet.Response;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

import org.apache.commons.lang.StringEscapeUtils;

abstract public class AbstractEchoService implements FakeInternetService {

  /**
   * Helper class for easier delegation of error responses in {@link AbstractEchoService}.
   */
  protected static class ErrorResponseException extends Exception {

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
      sb.append("<hei-id>").append(StringEscapeUtils.escapeXml(heiId)).append("</hei-id>");
    }
    for (String echo : echos) {
      sb.append("<echo>").append(StringEscapeUtils.escapeXml(echo)).append("</echo>");
    }
    sb.append("</response>");
    return new Response(request, 200, sb.toString().getBytes(StandardCharsets.UTF_8));
  }

  protected Response createErrorResponse(Request request, int status, String developerMessage) {
    StringBuilder sb = new StringBuilder();
    String NS = KnownNamespace.COMMON_TYPES_V1.getNamespaceUri();
    sb.append("<error-response xmlns='").append(NS).append("'>");
    sb.append("<developer-message>");
    sb.append(StringEscapeUtils.escapeXml(developerMessage));
    sb.append("</developer-message>");
    sb.append("</error-response>");
    return new Response(request, status, sb.toString().getBytes(StandardCharsets.UTF_8));
  }

  abstract protected Response handleInternetRequest2(Request request)
      throws IOException, ErrorResponseException;
}
