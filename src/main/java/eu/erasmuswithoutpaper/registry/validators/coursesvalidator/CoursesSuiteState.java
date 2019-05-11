package eu.erasmuswithoutpaper.registry.validators.coursesvalidator;

import eu.erasmuswithoutpaper.registry.validators.SemanticVersion;
import eu.erasmuswithoutpaper.registry.validators.SuiteState;

public class CoursesSuiteState extends SuiteState {
  public int maxLosIds;
  public int maxLosCodes;
  public String selectedHeiId;
  public String selectedLosId;

  public CoursesSuiteState(String url, SemanticVersion version) {
    super(url, version);
  }
}
