package eu.erasmuswithoutpaper.registry.echovalidator;

import java.security.KeyPair;

import eu.erasmuswithoutpaper.registry.internet.Internet.Request;
import eu.erasmuswithoutpaper.registry.internet.Internet.Response;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

/**
 * This one is invalid, because it includes a bad X-Request-Signature header.
 */
public class ServiceMMTTInvalid7 extends ServiceMMTTValid {

  public ServiceMMTTInvalid7(String url, RegistryClient registryClient, String myKeyId,
      KeyPair myKeyPair) {
    super(url, registryClient, myKeyId, myKeyPair);
  }

  @Override
  protected void includeProperHeaders(Request request, Response response) {
    super.includeProperHeaders(request, response);
    if (response.getHeader("X-Request-Signature") != null) {
      response.putHeader("X-Request-Signature", "Bad Signature");
      // Sign again.
      this.includeSignatureHeader(response);
    }
  }
}
