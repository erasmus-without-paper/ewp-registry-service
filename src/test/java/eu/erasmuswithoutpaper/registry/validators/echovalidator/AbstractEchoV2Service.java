package eu.erasmuswithoutpaper.registry.validators.echovalidator;

import java.security.KeyPair;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import eu.erasmuswithoutpaper.registry.documentbuilder.KnownNamespace;
import eu.erasmuswithoutpaper.registry.internet.InternetTestHelpers;
import eu.erasmuswithoutpaper.registry.internet.Request;
import eu.erasmuswithoutpaper.registry.internet.Response;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

abstract public class AbstractEchoV2Service extends AbstractEchoService {

  protected final List<KeyPair> serverKeys;

  public AbstractEchoV2Service(String url, RegistryClient registryClient) {
    super(url, registryClient);
    this.serverKeys = new ArrayList<>();
  }

  public AbstractEchoV2Service(String url, RegistryClient registryClient,
      List<KeyPair> serverKeys) {
    super(url, registryClient);
    this.serverKeys = serverKeys;
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
