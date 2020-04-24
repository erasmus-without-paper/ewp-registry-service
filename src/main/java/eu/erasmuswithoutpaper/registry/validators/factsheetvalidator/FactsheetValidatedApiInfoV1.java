package eu.erasmuswithoutpaper.registry.validators.factsheetvalidator;

import eu.erasmuswithoutpaper.registry.documentbuilder.KnownElement;
import eu.erasmuswithoutpaper.registry.documentbuilder.KnownNamespace;
import eu.erasmuswithoutpaper.registry.validators.ApiEndpoint;
import eu.erasmuswithoutpaper.registry.validators.ValidatedApiInfo;

class FactsheetValidatedApiInfoV1 implements ValidatedApiInfo {
  @Override
  public KnownElement getResponseKnownElement() {
    return KnownElement.RESPONSE_FACTSHEET_V1;
  }

  @Override
  public KnownNamespace getApiEntryKnownNamespace() {
    return KnownNamespace.APIENTRY_FACTSHEET_V1;
  }

  @Override
  public String getApiName() {
    return "factsheet";
  }

  @Override
  public ApiEndpoint getEndpoint() {
    return ApiEndpoint.NoEndpoint;
  }
}
