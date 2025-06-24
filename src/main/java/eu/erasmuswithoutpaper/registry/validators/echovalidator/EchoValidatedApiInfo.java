package eu.erasmuswithoutpaper.registry.validators.echovalidator;

import eu.erasmuswithoutpaper.registry.validators.ApiEndpoint;
import eu.erasmuswithoutpaper.registry.validators.ValidatedApiInfo;

class EchoValidatedApiInfo implements ValidatedApiInfo {
  private final int version;
  private final ApiEndpoint endpoint;

  public EchoValidatedApiInfo(int version, ApiEndpoint endpoint) {
    this.version = version;
    this.endpoint = endpoint;
  }

  @Override
  public String getElementName() {
    return "response";
  }

  @Override
  public int getVersion() {
    return this.version;
  }

  @Override
  public String getPreferredPrefix() {
    return "e";
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
    return "echo";
  }

  @Override
  public ApiEndpoint getEndpoint() {
    return this.endpoint;
  }
}
