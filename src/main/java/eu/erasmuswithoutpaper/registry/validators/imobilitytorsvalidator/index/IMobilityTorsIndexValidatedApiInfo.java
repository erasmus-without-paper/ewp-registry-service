package eu.erasmuswithoutpaper.registry.validators.imobilitytorsvalidator.index;

import eu.erasmuswithoutpaper.registry.documentbuilder.KnownElement;
import eu.erasmuswithoutpaper.registry.documentbuilder.KnownNamespace;
import eu.erasmuswithoutpaper.registry.validators.ApiEndpoint;
import eu.erasmuswithoutpaper.registry.validators.ValidatedApiInfo;

class IMobilityTorsIndexValidatedApiInfo implements ValidatedApiInfo {
  private Integer version;

  IMobilityTorsIndexValidatedApiInfo(Integer version) {
    this.version = version;
  }

  @Override
  public KnownElement getResponseKnownElement() {
    if (version == 1) {
      return KnownElement.RESPONSE_IMOBILITY_TORS_INDEX_V1;
    } else {
      return KnownElement.RESPONSE_IMOBILITY_TORS_INDEX_V2;
    }
  }

  @Override
  public KnownNamespace getApiEntryKnownNamespace() {
    if (version == 1) {
      return KnownNamespace.APIENTRY_IMOBILITY_TORS_V1;
    } else {
      return KnownNamespace.APIENTRY_IMOBILITY_TORS_V2;
    }
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
