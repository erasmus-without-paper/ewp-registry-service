package eu.erasmuswithoutpaper.registry.validators.mtdictionariesvalidator;

import eu.erasmuswithoutpaper.registry.validators.SemanticVersion;
import eu.erasmuswithoutpaper.registry.validators.SuiteState;

public class MtDictionariesSuiteState extends SuiteState {
  public String selectedDictionary;
  public String selectedCallYear;

  public MtDictionariesSuiteState(String url, SemanticVersion version) {
    super(url, version);
  }
}
