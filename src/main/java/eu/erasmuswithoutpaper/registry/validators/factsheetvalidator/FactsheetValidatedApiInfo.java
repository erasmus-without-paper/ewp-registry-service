package eu.erasmuswithoutpaper.registry.validators.factsheetvalidator;

import eu.erasmuswithoutpaper.registry.validators.ApiEndpoint;
import eu.erasmuswithoutpaper.registry.validators.ValidatedApiInfo;

class FactsheetValidatedApiInfo extends ValidatedApiInfo {
  private final int version;
  private final ApiEndpoint endpoint;

  public FactsheetValidatedApiInfo(int version, ApiEndpoint endpoint) {
    this.version = version;
    this.endpoint = endpoint;
  }

  @Override
  public int getVersion() {
    return this.version;
  }

  @Override
  public String preferredPrefix() {
    return "f";
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
    return "factsheet";
  }

  @Override
  public ApiEndpoint getEndpoint() {
    return this.endpoint;
  }
}
