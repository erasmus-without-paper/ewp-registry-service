package eu.erasmuswithoutpaper.registry.validators.omobilitylasvalidator.update;

import eu.erasmuswithoutpaper.registry.documentbuilder.KnownElement;
import eu.erasmuswithoutpaper.registry.documentbuilder.KnownNamespace;
import eu.erasmuswithoutpaper.registry.validators.ApiEndpoint;
import eu.erasmuswithoutpaper.registry.validators.ValidatedApiInfo;

public class OMobilityLAsUpdateValidatedApiInfo implements ValidatedApiInfo {
  @Override
  public KnownElement getResponseKnownElement() {
    return KnownElement.RESPONSE_OMOBILITY_LAS_UPDATE_V1;
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
    return ApiEndpoint.Update;
  }
}
