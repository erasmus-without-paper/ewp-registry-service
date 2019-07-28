package eu.erasmuswithoutpaper.registry.validators.imobilitytorsvalidator.index;

import eu.erasmuswithoutpaper.registry.documentbuilder.KnownElement;
import eu.erasmuswithoutpaper.registry.documentbuilder.KnownNamespace;
import eu.erasmuswithoutpaper.registry.validators.ApiEndpoint;
import eu.erasmuswithoutpaper.registry.validators.ValidatedApiInfo;

class IMobilityTorsIndexValidatedApiInfo implements ValidatedApiInfo {
  @Override
  public KnownElement getResponseKnownElement() {
    return KnownElement.RESPONSE_IMOBILITY_TORS_INDEX_V1;
  }

  @Override
  public KnownNamespace getApiEntryKnownNamespace() {
    return KnownNamespace.APIENTRY_IMOBILITY_TORS_V1;
  }

  @Override
  public String getApiName() {
    return "imobility-tors";
  }

  @Override
  public ApiEndpoint getEndpoint() {
    return ApiEndpoint.Index;
  }
}
