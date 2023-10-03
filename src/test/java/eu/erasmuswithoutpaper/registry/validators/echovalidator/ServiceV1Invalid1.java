package eu.erasmuswithoutpaper.registry.validators.echovalidator;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import eu.erasmuswithoutpaper.registry.internet.InternetTestHelpers;
import eu.erasmuswithoutpaper.registry.internet.Request;
import eu.erasmuswithoutpaper.registry.internet.Response;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

/**
 * Internal "fake" implementation of a slightly invalid Version 1 Echo API endpoint.
 *
 * <p>
 * Invalid because: 1. Accepts HTTP methods other than GET and POST.
 * </p>
 */
public class ServiceV1Invalid1 extends AbstractEchoV1Service {

  public ServiceV1Invalid1(String url, RegistryClient registryClient) {
    super(url, registryClient);
  }

  @Override
  public Response handleInternetRequest2(Request request) throws IOException {
    if (!request.getUrl().startsWith(this.myEndpoint)) {
      return null;
    }
    List<String> echos;
    try {
      echos = InternetTestHelpers.extractParams(request, "echo");
    } catch (RuntimeException e) {
      echos = Collections.emptyList();
    }
    return this.createEchoResponse(request, echos, Collections.emptyList());
  }
}
