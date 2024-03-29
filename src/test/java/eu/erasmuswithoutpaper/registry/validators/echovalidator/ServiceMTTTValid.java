package eu.erasmuswithoutpaper.registry.validators.echovalidator;

import java.io.IOException;

import eu.erasmuswithoutpaper.registry.internet.Request;
import eu.erasmuswithoutpaper.registry.internet.Response;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

/**
 * Internal "fake" implementation of a valid [SH]TTT Echo API endpoint (and endpoint which supports
 * both STTT and HTTT combinations).
 */
public class ServiceMTTTValid extends AbstractEchoV2Service {
  protected final ServiceHTTTValid httt;

  public ServiceMTTTValid(String url, RegistryClient registryClient) {
    super(url, registryClient);
    this.httt = new ServiceHTTTValid(url, registryClient);
  }

  @Override
  public Response handleInternetRequest2(Request request)
      throws IOException, ErrorResponseException {
    if (!request.getUrl().startsWith(this.myEndpoint)) {
      return null;
    }
    return this.httt.handleInternetRequest2(request);
  }

}
