package eu.erasmuswithoutpaper.registry.echovalidator;

import java.security.KeyPair;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import eu.erasmuswithoutpaper.registry.internet.Internet.Request;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

public class ServiceMMTTValid extends ServiceMHTTValid {

  public ServiceMMTTValid(String url, RegistryClient registryClient, String myKeyId,
      KeyPair myKeyPair) {
    super(url, registryClient, myKeyId, myKeyPair);
  }

  @Override
  protected boolean decideIfWeWantToSign(Request request) {
    /*
     * This implementation differs from its parent in that it checks if the client actually wants us
     * to sign the response. If he doesn't, then we don't sign it.
     */
    String value = request.getHeader("Accept-Signature");
    if (value == null) {
      // No such header. Per spec, that's not an error.
      return false;
    }
    Set<String> algorithms = Arrays.asList(value.split(",")).stream().map(String::trim)
        .map(String::toLowerCase).collect(Collectors.toSet());
    return algorithms.contains("rsa-sha256");
  }
}
