package eu.erasmuswithoutpaper.registry.echovalidator;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;

import eu.erasmuswithoutpaper.registry.documentbuilder.KnownNamespace;
import eu.erasmuswithoutpaper.registry.internet.FakeInternetService;
import eu.erasmuswithoutpaper.registry.internet.Internet.Response;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

import org.apache.commons.lang.StringEscapeUtils;

abstract public class AbstractEchoService implements FakeInternetService {

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

  protected Response createEchoResponse(String xmlns, List<String> echos,
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
    return new Response(200, sb.toString().getBytes(StandardCharsets.UTF_8));
  }

  protected Response createErrorResponse(int status, String developerMessage) {
    StringBuilder sb = new StringBuilder();
    String NS = KnownNamespace.COMMON_TYPES_V1.getNamespaceUri();
    sb.append("<error-response xmlns='").append(NS).append("'>");
    sb.append("<developer-message>");
    sb.append(StringEscapeUtils.escapeXml(developerMessage));
    sb.append("</developer-message>");
    sb.append("</error-response>");
    return new Response(status, sb.toString().getBytes(StandardCharsets.UTF_8));
  }
}
