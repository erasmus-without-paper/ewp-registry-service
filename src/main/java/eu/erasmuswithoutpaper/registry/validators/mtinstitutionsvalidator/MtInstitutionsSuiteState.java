package eu.erasmuswithoutpaper.registry.validators.mtinstitutionsvalidator;

import eu.erasmuswithoutpaper.registry.validators.SemanticVersion;
import eu.erasmuswithoutpaper.registry.validators.SuiteState;

public class MtInstitutionsSuiteState extends SuiteState {
  public String selectedPic;
  public String selectedEcheAtDate;
  public int maxIds;

  public MtInstitutionsSuiteState(String url, SemanticVersion version) {
    super(url, version);
  }
}
