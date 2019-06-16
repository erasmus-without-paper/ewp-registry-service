package eu.erasmuswithoutpaper.registry.validators.ounitsvalidator;

import eu.erasmuswithoutpaper.registry.documentbuilder.KnownElement;
import eu.erasmuswithoutpaper.registry.documentbuilder.KnownNamespace;
import eu.erasmuswithoutpaper.registry.validators.ValidatedApiInfo;

class OUnitsValidatedApiInfo implements ValidatedApiInfo {
  @Override
  public KnownElement getKnownElement() {
    return KnownElement.RESPONSE_OUNITS_V2;
  }

  @Override
  public String getApiNamespace() {
    return KnownNamespace.APIENTRY_OUNITS_V2.getNamespaceUri();
  }

  @Override
  public String getApiName() {
    return "organizational-units";
  }

  @Override
  public String getApiPrefix() {
    return "ou2";
  }

  @Override
  public String getApiResponsePrefix() {
    return "our2";
  }

  @Override
  public String getEndpoint() {
    return NO_ENDPOINT;
  }
}
