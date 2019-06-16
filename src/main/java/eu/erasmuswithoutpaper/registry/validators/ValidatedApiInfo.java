package eu.erasmuswithoutpaper.registry.validators;

import eu.erasmuswithoutpaper.registry.documentbuilder.KnownElement;

public interface ValidatedApiInfo {
  String NO_ENDPOINT = null;

  KnownElement getKnownElement();

  String getApiNamespace();

  String getApiName();

  String getApiPrefix();

  String getApiResponsePrefix();

  String getEndpoint();
}
