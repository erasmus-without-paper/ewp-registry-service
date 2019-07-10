package eu.erasmuswithoutpaper.registry.validators.mtprojectsvalidator;

import eu.erasmuswithoutpaper.registry.validators.SemanticVersion;
import eu.erasmuswithoutpaper.registry.validators.SuiteState;

public class MtProjectsSuiteState extends SuiteState {
  public String selectedPic;
  public String selectedCallYear;

  public MtProjectsSuiteState(String url, SemanticVersion version) {
    super(url, version);
  }
}
