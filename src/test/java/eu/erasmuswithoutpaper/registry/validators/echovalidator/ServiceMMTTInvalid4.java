package eu.erasmuswithoutpaper.registry.validators.echovalidator;

import java.io.IOException;
import java.security.KeyPair;

import eu.erasmuswithoutpaper.registry.internet.Request;
import eu.erasmuswithoutpaper.registry.internet.Response;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

/**
 * This one is invalid, because it adds whitespace to the end of all responses, thus invalidating
 * the digest.
 */
public class ServiceMMTTInvalid4 extends ServiceMMTTValid {

  public ServiceMMTTInvalid4(String url, RegistryClient registryClient, KeyPair myKeyPair) {
    super(url, registryClient, myKeyPair);
  }

  @Override
  public Response handleInternetRequest2(Request request)
      throws IOException, ErrorResponseException {
    Response response = super.handleInternetRequest2(request);
    if (response == null) {
      return null;
    }
    byte[] prev = response.getBody();
    byte[] changed = new byte[prev.length + 1];
    System.arraycopy(prev, 0, changed, 0, prev.length);
    changed[prev.length] = '\n';
    response.setBody(changed);
    return response;
  }
}
