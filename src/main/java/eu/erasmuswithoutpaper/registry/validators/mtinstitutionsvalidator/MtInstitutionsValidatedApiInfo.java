package eu.erasmuswithoutpaper.registry.validators.mtinstitutionsvalidator;

import eu.erasmuswithoutpaper.registry.validators.ApiEndpoint;
import eu.erasmuswithoutpaper.registry.validators.ValidatedApiInfo;

class MtInstitutionsValidatedApiInfo extends ValidatedApiInfo {
  private final int version;
  private final ApiEndpoint endpoint;

  public MtInstitutionsValidatedApiInfo(int version, ApiEndpoint endpoint) {
    this.version = version;
    this.endpoint = endpoint;
  }

  @Override
  public int getVersion() {
    return this.version;
  }

  @Override
  public String preferredPrefix() {
    return "mti";
  }

  @Override
  public boolean responseIncludeInCatalogueXmlns() {
    return false;
  }

  @Override
  public boolean apiEntryIncludeInCatalogueXmlns() {
    return false;
  }

  @Override
  public String getApiName() {
    return "mt-institutions";
  }

  @Override
  public ApiEndpoint getEndpoint() {
    return this.endpoint;
  }
}
