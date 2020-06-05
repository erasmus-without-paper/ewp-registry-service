package eu.erasmuswithoutpaper.registry.validators.imobilitiesvalidator.get;

import eu.erasmuswithoutpaper.registry.documentbuilder.KnownElement;
import eu.erasmuswithoutpaper.registry.documentbuilder.KnownNamespace;
import eu.erasmuswithoutpaper.registry.validators.ApiEndpoint;
import eu.erasmuswithoutpaper.registry.validators.ValidatedApiInfo;

class IMobilitiesGetValidatedApiInfo implements ValidatedApiInfo {
  @Override
  public KnownElement getResponseKnownElement() {
    return KnownElement.RESPONSE_IMOBILITIES_GET_V1;
  }

  @Override
  public KnownNamespace getApiEntryKnownNamespace() {
    return KnownNamespace.APIENTRY_IMOBILITIES_V1;
  }

  @Override
  public String getApiName() {
    return "imobilities";
  }

  @Override
  public ApiEndpoint getEndpoint() {
    return ApiEndpoint.Get;
  }
}
