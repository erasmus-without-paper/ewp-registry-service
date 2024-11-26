package eu.erasmuswithoutpaper.registry.validators.omobilitylascnrvalidator;

import eu.erasmuswithoutpaper.registry.validators.SemanticVersion;
import eu.erasmuswithoutpaper.registry.validators.SuiteState;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@SuppressFBWarnings("UUF_UNUSED_PUBLIC_OR_PROTECTED_FIELD")
public class OMobilityLaCnrSuiteState extends SuiteState {
  public String sendingHeiId;
  public String omobilityId;

  public OMobilityLaCnrSuiteState(String url, SemanticVersion version) {
    super(url, version);
  }
}
