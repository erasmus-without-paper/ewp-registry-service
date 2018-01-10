package eu.erasmuswithoutpaper.registry.echovalidator;

import java.security.KeyPair;

import eu.erasmuswithoutpaper.registry.internet.Request;
import eu.erasmuswithoutpaper.registry.internet.Response;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

/**
 * This one is invalid, because it never includes the X-Request-Signature header.
 */
public class ServiceMMTTInvalid6 extends ServiceMMTTValid {

  public ServiceMMTTInvalid6(String url, RegistryClient registryClient, String myKeyId,
      KeyPair myKeyPair) {
    super(url, registryClient, myKeyId, myKeyPair);
  }

  @Override
  protected void includeProperHeaders(Request request, Response response) {
    super.includeProperHeaders(request, response);
    response.removeHeader("X-Request-Signature");
    // Sign again.
    this.includeSignatureHeader(response);
  }
}
