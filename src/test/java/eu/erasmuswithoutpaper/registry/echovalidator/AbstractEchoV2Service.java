package eu.erasmuswithoutpaper.registry.echovalidator;

import java.util.Collection;
import java.util.List;

import eu.erasmuswithoutpaper.registry.documentbuilder.KnownNamespace;
import eu.erasmuswithoutpaper.registry.internet.InternetTestHelpers;
import eu.erasmuswithoutpaper.registry.internet.Request;
import eu.erasmuswithoutpaper.registry.internet.Response;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

abstract public class AbstractEchoV2Service extends AbstractEchoService {

  public AbstractEchoV2Service(String url, RegistryClient registryClient) {
    super(url, registryClient);
  }

  protected Response createEchoResponse(Request request, List<String> echos,
      Collection<String> heiIds) {
    return this.createEchoResponse(request, KnownNamespace.RESPONSE_ECHO_V2.getNamespaceUri(),
        echos, heiIds);
  }


  protected List<String> retrieveEchoValues(Request request) {
    return InternetTestHelpers.extractParams(request, "echo");
  }

}
