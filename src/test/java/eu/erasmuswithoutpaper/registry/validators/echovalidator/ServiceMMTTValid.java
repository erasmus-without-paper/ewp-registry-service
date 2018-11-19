package eu.erasmuswithoutpaper.registry.validators.echovalidator;

import java.security.KeyPair;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import eu.erasmuswithoutpaper.registry.internet.Request;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

public class ServiceMMTTValid extends ServiceMHTTValid {

  public ServiceMMTTValid(String url, RegistryClient registryClient, KeyPair myKeyPair) {
    super(url, registryClient, myKeyPair);
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
    Set<String> algorithms = Arrays.asList(value.split(",")).stream()
        .map(s -> s.trim().toLowerCase(Locale.US)).collect(Collectors.toSet());
    return algorithms.contains("rsa-sha256");
  }
}
