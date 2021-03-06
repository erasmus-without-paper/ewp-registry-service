package eu.erasmuswithoutpaper.registry.validators.iiavalidator.v2.get;

import eu.erasmuswithoutpaper.registry.documentbuilder.KnownElement;
import eu.erasmuswithoutpaper.registry.documentbuilder.KnownNamespace;
import eu.erasmuswithoutpaper.registry.validators.ApiEndpoint;
import eu.erasmuswithoutpaper.registry.validators.ValidatedApiInfo;

class IiaGetValidatedApiInfoV2 implements ValidatedApiInfo {
  @Override
  public KnownElement getResponseKnownElement() {
    return KnownElement.RESPONSE_IIAS_GET_V2;
  }

  @Override
  public KnownNamespace getApiEntryKnownNamespace() {
    return KnownNamespace.APIENTRY_IIAS_V2;
  }

  @Override
  public String getApiName() {
    return "iias";
  }

  @Override
  public ApiEndpoint getEndpoint() {
    return ApiEndpoint.Get;
  }
}
