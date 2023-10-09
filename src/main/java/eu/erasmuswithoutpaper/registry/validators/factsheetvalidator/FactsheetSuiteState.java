package eu.erasmuswithoutpaper.registry.validators.factsheetvalidator;

import eu.erasmuswithoutpaper.registry.validators.SemanticVersion;
import eu.erasmuswithoutpaper.registry.validators.SuiteState;

public class FactsheetSuiteState extends SuiteState {
  private String selectedHeiId;
  private int maxHeiIds;

  public FactsheetSuiteState(String url, SemanticVersion version) {
    super(url, version);
  }

  public String getSelectedHeiId() {
    return selectedHeiId;
  }

  public void setSelectedHeiId(String selectedHeiId) {
    this.selectedHeiId = selectedHeiId;
  }

  public int getMaxHeiIds() {
    return maxHeiIds;
  }

  public void setMaxHeiIds(int maxHeiIds) {
    this.maxHeiIds = maxHeiIds;
  }
}
