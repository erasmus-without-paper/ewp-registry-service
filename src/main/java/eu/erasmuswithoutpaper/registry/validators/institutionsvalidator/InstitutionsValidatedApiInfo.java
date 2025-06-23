package eu.erasmuswithoutpaper.registry.validators.institutionsvalidator;

import eu.erasmuswithoutpaper.registry.validators.ApiEndpoint;
import eu.erasmuswithoutpaper.registry.validators.ValidatedApiInfo;

class InstitutionsValidatedApiInfo extends ValidatedApiInfo {
  private final int version;

  public InstitutionsValidatedApiInfo(int version) {
    this.version = version;
  }

  @Override
  public int getVersion() {
    return this.version;
  }

  @Override
  public String getPreferredPrefix() {
    return "in";
  }

  @Override
  public boolean responseIncludeInCatalogueXmlns() {
    return false;
  }

  @Override
  public boolean apiEntryIncludeInCatalogueXmlns() {
    return this.version == 2;
  }

  @Override
  public String getApiName() {
    return "institutions";
  }

  @Override
  public ApiEndpoint getEndpoint() {
    return ApiEndpoint.NO_ENDPOINT;
  }
}
