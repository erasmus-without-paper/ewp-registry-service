package eu.erasmuswithoutpaper.registry.validators.institutionsvalidator;

import eu.erasmuswithoutpaper.registry.validators.SemanticVersion;
import eu.erasmuswithoutpaper.registry.validators.SuiteState;

public class InstitutionsSuiteState extends SuiteState {
  public int maxHeiIds = 0;

  public InstitutionsSuiteState(String url, SemanticVersion version) {
    super(url, version);
  }
}
