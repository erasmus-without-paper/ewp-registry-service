package eu.erasmuswithoutpaper.registry.echovalidator;

import java.security.KeyPair;

import eu.erasmuswithoutpaper.registry.internet.Request;
import eu.erasmuswithoutpaper.registry.internet.Response;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

/**
 * This one is invalid, because it never includes the X-Request-Id header.
 */
public class ServiceMMTTInvalid5 extends ServiceMMTTValid {

  public ServiceMMTTInvalid5(String url, RegistryClient registryClient, String myKeyId,
      KeyPair myKeyPair) {
    super(url, registryClient, myKeyId, myKeyPair);
  }

  @Override
  protected void includeProperHeaders(Request request, Response response) {
    super.includeProperHeaders(request, response);
    response.removeHeader("X-Request-Id");
    // Sign again.
    this.includeSignatureHeader(request, response);
  }
}
