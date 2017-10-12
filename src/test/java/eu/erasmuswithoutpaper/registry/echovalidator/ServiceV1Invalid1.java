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
 * Invalid because: 1. Accepts HTTP methods other than GET and POST. 2. It doesn't validate the
 * client certificate (trusts any).
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
    if (!request.getClientCertificate().isPresent()) {
      return this.createErrorResponse(request, 403, "Expecting client certificate.");
    }
    List<String> echos;
    try {
      echos = InternetTestHelpers.extractParams(request, "echo");
    } catch (RuntimeException e) {
      echos = Lists.newArrayList();
    }
    return this.createEchoResponse(request, echos, Lists.newArrayList());
  }
}
