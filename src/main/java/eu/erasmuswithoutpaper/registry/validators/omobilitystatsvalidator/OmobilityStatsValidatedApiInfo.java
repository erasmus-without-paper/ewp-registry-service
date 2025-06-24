package eu.erasmuswithoutpaper.registry.validators.omobilitystatsvalidator;

import eu.erasmuswithoutpaper.registry.validators.ApiEndpoint;
import eu.erasmuswithoutpaper.registry.validators.ValidatedApiInfoImpl;

class OmobilityStatsValidatedApiInfo extends ValidatedApiInfoImpl {

  public static final String NAME = "omobility-stats";

  public OmobilityStatsValidatedApiInfo(int version) {
    super(NAME, version, ApiEndpoint.NO_ENDPOINT, "oms");
  }

}
