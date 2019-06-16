package eu.erasmuswithoutpaper.registry.validators.echovalidator;

import eu.erasmuswithoutpaper.registry.documentbuilder.KnownElement;
import eu.erasmuswithoutpaper.registry.documentbuilder.KnownNamespace;
import eu.erasmuswithoutpaper.registry.validators.ValidatedApiInfo;

class EchoValidatedApiInfoV2 implements ValidatedApiInfo {
  @Override
  public String getApiName() {
    return "echo";
  }

  @Override
  public KnownElement getKnownElement() {
    return KnownElement.RESPONSE_ECHO_V2;
  }

  @Override
  public String getApiNamespace() {
    return KnownNamespace.APIENTRY_ECHO_V2.getNamespaceUri();
  }

  @Override
  public String getApiPrefix() {
    return "e2";
  }

  @Override
  public String getApiResponsePrefix() {
    return "er2";
  }

  @Override
  public String getEndpoint() {
    return NO_ENDPOINT;
  }
}
