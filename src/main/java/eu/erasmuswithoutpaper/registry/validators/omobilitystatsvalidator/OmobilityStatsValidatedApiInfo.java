package eu.erasmuswithoutpaper.registry.validators.omobilitystatsvalidator;

import eu.erasmuswithoutpaper.registry.validators.ApiEndpoint;
import eu.erasmuswithoutpaper.registry.validators.ValidatedApiInfo;

class OmobilityStatsValidatedApiInfo extends ValidatedApiInfo {
  public static final String NAME = "omobility-stats";

  private final int version;
  private final ApiEndpoint endpoint;

  public OmobilityStatsValidatedApiInfo(int version, ApiEndpoint endpoint) {
    this.version = version;
    this.endpoint = endpoint;
  }

  @Override
  public int getVersion() {
    return this.version;
  }

  @Override
  public String preferredPrefix() {
    return "oms";
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
    return NAME;
  }

  @Override
  public ApiEndpoint getEndpoint() {
    return this.endpoint;
  }
}
