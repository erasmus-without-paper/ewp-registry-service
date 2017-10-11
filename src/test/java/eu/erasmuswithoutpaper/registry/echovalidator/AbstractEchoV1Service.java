package eu.erasmuswithoutpaper.registry.echovalidator;

import java.util.Collection;
import java.util.List;

import eu.erasmuswithoutpaper.registry.documentbuilder.KnownNamespace;
import eu.erasmuswithoutpaper.registry.internet.Internet.Response;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

abstract public class AbstractEchoV1Service extends AbstractEchoService {

  public AbstractEchoV1Service(String url, RegistryClient registryClient) {
    super(url, registryClient);
  }

  protected Response createEchoResponse(List<String> echos, Collection<String> heiIds) {
    return this.createEchoResponse(KnownNamespace.RESPONSE_ECHO_V1.getNamespaceUri(), echos,
        heiIds);
  }

}
