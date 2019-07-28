package eu.erasmuswithoutpaper.registry.validators;

import eu.erasmuswithoutpaper.registry.documentbuilder.KnownElement;
import eu.erasmuswithoutpaper.registry.documentbuilder.KnownNamespace;

public interface ValidatedApiInfo {
  KnownElement getResponseKnownElement();

  KnownNamespace getApiEntryKnownNamespace();

  String getApiName();

  ApiEndpoint getEndpoint();

  default String getApiNamespace() {
    return getApiEntryKnownNamespace().getNamespaceUri();
  }

  default String getApiPrefix() {
    return getApiEntryKnownNamespace().getPreferredPrefix();
  }

  default String getResponsePrefix() {
    return getResponseKnownElement().getNamespacePreferredPrefix();
  }
}
