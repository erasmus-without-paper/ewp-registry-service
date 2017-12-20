package eu.erasmuswithoutpaper.registry.echovalidator;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import eu.erasmuswithoutpaper.registry.internet.Internet.Request;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

/**
 * This one is invalid, because it doesn't encrypt responses at all.
 */
public class ServiceSTTEInvalid3 extends ServiceSTTEValid {

  public ServiceSTTEInvalid3(String url, RegistryClient registryClient) {
    super(url, registryClient);
  }

  @Override
  protected List<String> getCodingsToApply(Request request) {
    List<String> result = super.getCodingsToApply(request).stream()
        .map(s -> s.toLowerCase(Locale.US)).collect(Collectors.toList());
    result.remove("ewp-rsa-aes128gcm");
    return result;
  }
}
