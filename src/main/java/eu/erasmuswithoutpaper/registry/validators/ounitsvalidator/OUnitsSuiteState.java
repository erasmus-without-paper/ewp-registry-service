package eu.erasmuswithoutpaper.registry.validators.ounitsvalidator;

import eu.erasmuswithoutpaper.registry.validators.SemanticVersion;
import eu.erasmuswithoutpaper.registry.validators.SuiteState;

public class OUnitsSuiteState extends SuiteState {
  public int maxOunitIds = 0;
  public int maxOunitCodes = 0;
  public String selectedHeiId;
  public String selectedOunitId;

  public OUnitsSuiteState(String url, SemanticVersion version) {
    super(url, version);
  }
}
