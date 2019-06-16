package eu.erasmuswithoutpaper.registry.validators.iiavalidator.get;

import eu.erasmuswithoutpaper.registry.documentbuilder.KnownElement;
import eu.erasmuswithoutpaper.registry.documentbuilder.KnownNamespace;
import eu.erasmuswithoutpaper.registry.validators.ValidatedApiInfo;

class IiaGetValidatedApiInfo implements ValidatedApiInfo {
  @Override
  public KnownElement getKnownElement() {
    return KnownElement.RESPONSE_IIAS_GET_V2;
  }

  @Override
  public String getApiNamespace() {
    return KnownNamespace.APIENTRY_IIAS_V2.getNamespaceUri();
  }

  @Override
  public String getApiName() {
    return "iias";
  }

  @Override
  public String getApiPrefix() {
    return "ia2";
  }

  @Override
  public String getApiResponsePrefix() {
    return "iarg2";
  }

  @Override
  public String getEndpoint() {
    return "get";
  }
}
