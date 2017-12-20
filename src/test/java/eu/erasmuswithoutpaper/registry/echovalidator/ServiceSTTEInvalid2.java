package eu.erasmuswithoutpaper.registry.echovalidator;

import java.util.ArrayList;
import java.util.List;

import eu.erasmuswithoutpaper.registry.internet.Internet.Request;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

/**
 * This one is invalid, because it always gzips responses. Even if the client doesn't accept it.
 */
public class ServiceSTTEInvalid2 extends ServiceSTTEValid {

  public ServiceSTTEInvalid2(String url, RegistryClient registryClient) {
    super(url, registryClient);
  }

  @Override
  protected List<String> getCodingsToApply(Request request) {
    List<String> result = new ArrayList<>(super.getCodingsToApply(request));
    result.add(0, "gzip");
    // Note, that this means that two "gzip"s MAY be present.
    return result;
  }
}
