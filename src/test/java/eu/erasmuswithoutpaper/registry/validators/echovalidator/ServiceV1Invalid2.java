package eu.erasmuswithoutpaper.registry.validators.echovalidator;

import java.io.IOException;
import java.util.List;

import eu.erasmuswithoutpaper.registry.internet.Request;
import eu.erasmuswithoutpaper.registry.internet.Response;
import eu.erasmuswithoutpaper.registry.internet.sec.EwpClientWithRsaKey;
import eu.erasmuswithoutpaper.registry.internet.sec.EwpHttpSigRequestAuthorizer;
import eu.erasmuswithoutpaper.registry.internet.sec.Http4xx;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

import org.assertj.core.util.Lists;

/**
 * Internal "fake" implementation of an INVALID Version 1 Echo API endpoint.
 *
 * <p>
 * Invalid because: 1. Accepts HTTP methods other than GET and POST. 2. Always returns a hardcoded
 * set of echo values.
 * </p>
 */
public class ServiceV1Invalid2 extends AbstractEchoV1Service {

  public ServiceV1Invalid2(String url, RegistryClient registryClient) {
    super(url, registryClient);
  }

  @Override
  public Response handleInternetRequest2(Request request) throws IOException {
    if (!request.getUrl().startsWith(this.myEndpoint)) {
      return null;
    }
    List<String> echos = Lists.newArrayList("a", "b");
    EwpClientWithRsaKey client;
    try {
      client = new EwpHttpSigRequestAuthorizer(this.registryClient).authorize(request);
    } catch (Http4xx e) {
      return e.generateEwpErrorResponse();
    }

    return this.createEchoResponse(request, echos,
        this.registryClient.getHeisCoveredByClientKey(client.getRsaPublicKey()));
  }

}
