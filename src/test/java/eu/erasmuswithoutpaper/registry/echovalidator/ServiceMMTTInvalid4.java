package eu.erasmuswithoutpaper.registry.echovalidator;

import java.io.IOException;
import java.security.KeyPair;

import eu.erasmuswithoutpaper.registry.internet.Internet.Request;
import eu.erasmuswithoutpaper.registry.internet.Internet.Response;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

/**
 * This one is invalid, because it adds whitespace to the end of all responses, thus invalidating
 * the digest.
 */
public class ServiceMMTTInvalid4 extends ServiceMMTTValid {

  public ServiceMMTTInvalid4(String url, RegistryClient registryClient, String myKeyId,
      KeyPair myKeyPair) {
    super(url, registryClient, myKeyId, myKeyPair);
  }

  @Override
  public Response handleInternetRequest2(Request request)
      throws IOException, ErrorResponseException {
    Response response = super.handleInternetRequest2(request);
    byte[] prev = response.getBody();
    byte[] changed = new byte[prev.length + 1];
    System.arraycopy(prev, 0, changed, 0, prev.length);
    changed[prev.length] = '\n';
    response.setBody(changed);
    return response;
  }
}
