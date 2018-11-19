package eu.erasmuswithoutpaper.registry.validators.echovalidator;

import java.util.List;

import eu.erasmuswithoutpaper.registry.internet.Request;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

import com.google.common.collect.Lists;

/**
 * A bit invalid, because - when the client accepts both encryption AND gzip - then it first
 * encrypts responses, and then gzips them (inefficient order of operations).
 */
public class ServiceSTTEInvalid1 extends ServiceSTTEValid {

  public ServiceSTTEInvalid1(String url, RegistryClient registryClient) {
    super(url, registryClient);
  }

  @Override
  protected List<String> getCodingsToApply(Request request) {
    // Reverse all coding orders from the super class.
    return Lists.reverse(super.getCodingsToApply(request));
  }
}
