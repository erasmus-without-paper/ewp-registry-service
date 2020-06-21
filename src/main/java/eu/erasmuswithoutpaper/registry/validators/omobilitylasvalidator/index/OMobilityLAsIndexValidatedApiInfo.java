package eu.erasmuswithoutpaper.registry.validators.omobilitylasvalidator.index;

import eu.erasmuswithoutpaper.registry.documentbuilder.KnownElement;
import eu.erasmuswithoutpaper.registry.documentbuilder.KnownNamespace;
import eu.erasmuswithoutpaper.registry.validators.ApiEndpoint;
import eu.erasmuswithoutpaper.registry.validators.ValidatedApiInfo;

class OMobilityLAsIndexValidatedApiInfo implements ValidatedApiInfo {
  @Override
  public KnownElement getResponseKnownElement() {
    return KnownElement.RESPONSE_OMOBILITY_LAS_INDEX_V1;
  }

  @Override
  public KnownNamespace getApiEntryKnownNamespace() {
    return KnownNamespace.APIENTRY_OMOBILITY_LAS_V1;
  }

  @Override
  public String getApiName() {
    return "omobility-las";
  }

  @Override
  public ApiEndpoint getEndpoint() {
    return ApiEndpoint.Index;
  }
}
