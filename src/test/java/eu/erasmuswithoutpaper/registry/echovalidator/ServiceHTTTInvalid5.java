package eu.erasmuswithoutpaper.registry.echovalidator;

import eu.erasmuswithoutpaper.registry.internet.Internet.Request;
import eu.erasmuswithoutpaper.registry.internet.Internet.Response;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

/**
 * This one is a bit invalid, because it doesn't include the Want-Digest header in HTTP 401
 * responses.
 */
public class ServiceHTTTInvalid5 extends ServiceHTTTValid {

  public ServiceHTTTInvalid5(String url, RegistryClient registryClient) {
    super(url, registryClient);
  }

  @Override
  protected Response createHttpSig401Response(Request request) {
    Response response = super.createHttpSig401Response(request);
    response.removeHeader("Want-Digest");
    return response;
  }
}
