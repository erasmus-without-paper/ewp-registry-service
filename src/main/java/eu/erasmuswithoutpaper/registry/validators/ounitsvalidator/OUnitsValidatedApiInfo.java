package eu.erasmuswithoutpaper.registry.validators.ounitsvalidator;

import eu.erasmuswithoutpaper.registry.documentbuilder.KnownElement;
import eu.erasmuswithoutpaper.registry.documentbuilder.KnownNamespace;
import eu.erasmuswithoutpaper.registry.validators.ApiEndpoint;
import eu.erasmuswithoutpaper.registry.validators.ValidatedApiInfo;

class OUnitsValidatedApiInfo implements ValidatedApiInfo {
  @Override
  public KnownElement getResponseKnownElement() {
    return KnownElement.RESPONSE_OUNITS_V2;
  }

  @Override
  public KnownNamespace getApiEntryKnownNamespace() {
    return KnownNamespace.APIENTRY_OUNITS_V2;
  }

  @Override
  public String getApiName() {
    return "organizational-units";
  }

  @Override
  public ApiEndpoint getEndpoint() {
    return ApiEndpoint.NoEndpoint;
  }

  @Override
  public String getGitHubRepositoryName() {
    return "ounits";
  }
}
