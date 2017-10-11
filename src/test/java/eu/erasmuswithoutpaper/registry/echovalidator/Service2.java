package eu.erasmuswithoutpaper.registry.echovalidator;

import java.io.IOException;
import java.util.List;

import eu.erasmuswithoutpaper.registry.internet.Internet.Request;
import eu.erasmuswithoutpaper.registry.internet.Internet.Response;
import eu.erasmuswithoutpaper.registry.internet.InternetTestHelpers;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

import com.google.common.collect.Lists;

/**
 * Internal "fake" implementation of a slightly invalid Version 1 Echo API endpoint.
 *
 * <p>
 * Mistakes: It doesn't validate the client certificate.
 * </p>
 */
public class Service2 extends AbstractEchoV1Service {

  public Service2(String url, RegistryClient registryClient) {
    super(url, registryClient);
  }

  @Override
  public Response handleInternetRequest(Request request) throws IOException {
    if (!request.getUrl().startsWith(this.myEndpoint)) {
      return null;
    }
    if (!request.getClientCertificate().isPresent()) {
      return this.createErrorResponse(403, "Expecting client certificate.");
    }
    List<String> echos;
    try {
      echos = InternetTestHelpers.extractParams(request, "echo");
    } catch (RuntimeException e) {
      echos = Lists.newArrayList();
    }
    return this.createEchoResponse(echos, Lists.newArrayList());
  }
}
