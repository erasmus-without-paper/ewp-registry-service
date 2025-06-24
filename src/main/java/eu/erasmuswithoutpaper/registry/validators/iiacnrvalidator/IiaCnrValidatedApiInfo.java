package eu.erasmuswithoutpaper.registry.validators.iiacnrvalidator;

import eu.erasmuswithoutpaper.registry.validators.ApiEndpoint;
import eu.erasmuswithoutpaper.registry.validators.ValidatedApiInfo;

class IiaCnrValidatedApiInfo implements ValidatedApiInfo {
  private final int version;
  private final ApiEndpoint endpoint;

  public IiaCnrValidatedApiInfo(int version, ApiEndpoint endpoint) {
    this.version = version;
    this.endpoint = endpoint;
  }

  @Override
  public int getVersion() {
    return this.version;
  }

  @Override
  public String getPreferredPrefix() {
    return "iac";
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
    return "iia-cnr";
  }

  @Override
  public ApiEndpoint getEndpoint() {
    return this.endpoint;
  }
}
