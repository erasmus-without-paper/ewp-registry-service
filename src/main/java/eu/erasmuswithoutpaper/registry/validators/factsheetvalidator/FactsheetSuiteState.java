package eu.erasmuswithoutpaper.registry.validators.factsheetvalidator;

import eu.erasmuswithoutpaper.registry.validators.SemanticVersion;
import eu.erasmuswithoutpaper.registry.validators.SuiteState;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@SuppressFBWarnings("URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
public class FactsheetSuiteState extends SuiteState {
  public String selectedHeiId;
  public int maxHeiIds;

  public FactsheetSuiteState(String url, SemanticVersion version) {
    super(url, version);
  }
}
