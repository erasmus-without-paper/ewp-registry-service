package eu.erasmuswithoutpaper.registry.validators.mtdictionariesvalidator;

import eu.erasmuswithoutpaper.registry.validators.ApiEndpoint;
import eu.erasmuswithoutpaper.registry.validators.ValidatedApiInfo;

class MtDictionariesValidatedApiInfo implements ValidatedApiInfo {
  private final int version;
  private final ApiEndpoint endpoint;

  public MtDictionariesValidatedApiInfo(int version, ApiEndpoint endpoint) {
    this.version = version;
    this.endpoint = endpoint;
  }

  @Override
  public int getVersion() {
    return this.version;
  }

  @Override
  public String getPreferredPrefix() {
    return "mtd";
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
    return "mt-dictionaries";
  }

  @Override
  public ApiEndpoint getEndpoint() {
    return this.endpoint;
  }
}
