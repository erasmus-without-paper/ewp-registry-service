package eu.erasmuswithoutpaper.registry.validators.omobilitiesvalidator;

import eu.erasmuswithoutpaper.registry.validators.ApiEndpoint;
import eu.erasmuswithoutpaper.registry.validators.ValidatedApiInfo;

public class OMobilitiesValidatedApiInfo implements ValidatedApiInfo {
  private final int version;
  private final ApiEndpoint endpoint;

  public OMobilitiesValidatedApiInfo(int version, ApiEndpoint endpoint) {
    this.version = version;
    this.endpoint = endpoint;
  }

  @Override
  public int getVersion() {
    return this.version;
  }

  @Override
  public String getPreferredPrefix() {
    return "om";
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
    return "omobilities";
  }

  @Override
  public ApiEndpoint getEndpoint() {
    return this.endpoint;
  }
}
