package eu.erasmuswithoutpaper.registry.validators.imobilitytorsvalidator;

import eu.erasmuswithoutpaper.registry.validators.SemanticVersion;
import eu.erasmuswithoutpaper.registry.validators.SuiteState;

public class IMobilityTorsSuiteState extends SuiteState {
  public String sendingHeiId;
  public String receivingHeiId;
  public String omobilityId;
  public int maxOmobilityIds;
  public String notPermittedHeiId;

  public IMobilityTorsSuiteState(String url, SemanticVersion version) {
    super(url, version);
  }
}
