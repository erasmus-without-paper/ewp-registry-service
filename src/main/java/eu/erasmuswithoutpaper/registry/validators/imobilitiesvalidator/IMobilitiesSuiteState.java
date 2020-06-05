package eu.erasmuswithoutpaper.registry.validators.imobilitiesvalidator;

import eu.erasmuswithoutpaper.registry.validators.SemanticVersion;
import eu.erasmuswithoutpaper.registry.validators.SuiteState;

public class IMobilitiesSuiteState extends SuiteState {
  public String receivingHeiId;
  public String omobilityId;
  public int maxOmobilityIds;

  public IMobilitiesSuiteState(String url, SemanticVersion version) {
    super(url, version);
  }
}
