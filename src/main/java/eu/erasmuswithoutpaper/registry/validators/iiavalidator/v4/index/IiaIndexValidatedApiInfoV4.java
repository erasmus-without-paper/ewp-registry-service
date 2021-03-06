package eu.erasmuswithoutpaper.registry.validators.iiavalidator.v4.index;

import eu.erasmuswithoutpaper.registry.documentbuilder.KnownElement;
import eu.erasmuswithoutpaper.registry.documentbuilder.KnownNamespace;
import eu.erasmuswithoutpaper.registry.validators.ApiEndpoint;
import eu.erasmuswithoutpaper.registry.validators.ValidatedApiInfo;

class IiaIndexValidatedApiInfoV4 implements ValidatedApiInfo {
  @Override
  public KnownElement getResponseKnownElement() {
    return KnownElement.RESPONSE_IIAS_INDEX_V4;
  }

  @Override
  public KnownNamespace getApiEntryKnownNamespace() {
    return KnownNamespace.APIENTRY_IIAS_V4;
  }

  @Override
  public String getApiName() {
    return "iias";
  }

  @Override
  public ApiEndpoint getEndpoint() {
    return ApiEndpoint.Index;
  }
}
