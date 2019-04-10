package eu.erasmuswithoutpaper.registry.validators.coursesreplicationvalidator;

import eu.erasmuswithoutpaper.registry.validators.SemanticVersion;
import eu.erasmuswithoutpaper.registry.validators.SuiteState;

public class CourseReplicationSuiteState extends SuiteState {
  public String selectedHeiId;
  public boolean supportsModifiedSince;

  public CourseReplicationSuiteState(String url, SemanticVersion version) {
    super(url, version);
  }
}
