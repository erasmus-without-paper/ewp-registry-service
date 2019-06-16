package eu.erasmuswithoutpaper.registry.validators.institutionsvalidator;

import eu.erasmuswithoutpaper.registry.documentbuilder.KnownElement;
import eu.erasmuswithoutpaper.registry.documentbuilder.KnownNamespace;
import eu.erasmuswithoutpaper.registry.validators.ValidatedApiInfo;

class InstitutionsValidatedApiInfo implements ValidatedApiInfo {
  @Override
  public KnownElement getKnownElement() {
    return KnownElement.RESPONSE_INSTITUTIONS_V2;
  }

  @Override
  public String getApiNamespace() {
    return KnownNamespace.APIENTRY_INSTITUTIONS_V2.getNamespaceUri();
  }

  @Override
  public String getApiName() {
    return "institutions";
  }

  @Override
  public String getApiPrefix() {
    return "in2";
  }

  @Override
  public String getApiResponsePrefix() {
    return "inr2";
  }

  @Override
  public String getEndpoint() {
    return NO_ENDPOINT;
  }
}
