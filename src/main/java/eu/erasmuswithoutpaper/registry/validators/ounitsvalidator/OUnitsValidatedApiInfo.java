package eu.erasmuswithoutpaper.registry.validators.ounitsvalidator;

import eu.erasmuswithoutpaper.registry.validators.ApiEndpoint;
import eu.erasmuswithoutpaper.registry.validators.ValidatedApiInfo;

class OUnitsValidatedApiInfo extends ValidatedApiInfo {
  private final int version;
  private final ApiEndpoint endpoint;

  public OUnitsValidatedApiInfo(int version, ApiEndpoint endpoint) {
    this.version = version;
    this.endpoint = endpoint;
  }

  @Override
  public int getVersion() {
    return this.version;
  }

  @Override
  public String preferredPrefix() {
    return "ou";
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
    return "organizational-units";
  }

  @Override
  public ApiEndpoint getEndpoint() {
    return this.endpoint;
  }

  @Override
  public String getElementName() {
    return "ounits-response";
  }

  @Override
  public String getNamespaceApiName() {
    return "api-ounits";
  }

  @Override
  public String getGitHubRepositoryName() {
    return "ounits";
  }
}
