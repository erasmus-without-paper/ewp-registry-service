package eu.erasmuswithoutpaper.registry.echovalidator;

import java.security.KeyPair;

import eu.erasmuswithoutpaper.registry.internet.Internet.Request;
import eu.erasmuswithoutpaper.registry.internet.Internet.Response;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

/**
 * This one is invalid, because it always includes the X-Request-Id header, even when there was no
 * corresponding header in the request.
 */
public class ServiceMMTTInvalid9 extends ServiceMMTTValid {

  public ServiceMMTTInvalid9(String url, RegistryClient registryClient, String myKeyId,
      KeyPair myKeyPair) {
    super(url, registryClient, myKeyId, myKeyPair);
  }

  @Override
  protected void includeXRequestIdHeader(Request request, Response response) {
    super.includeXRequestIdHeader(request, response);
    if (response.getHeader("X-Request-Id") == null) {
      response.putHeader("X-Request-Id", ""); // empty, but exists.
    }
  }
}
