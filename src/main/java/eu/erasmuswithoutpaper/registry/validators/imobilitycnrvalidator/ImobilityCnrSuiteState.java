package eu.erasmuswithoutpaper.registry.validators.imobilitycnrvalidator;

import eu.erasmuswithoutpaper.registry.validators.SemanticVersion;
import eu.erasmuswithoutpaper.registry.validators.SuiteState;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@SuppressFBWarnings("UUF_UNUSED_PUBLIC_OR_PROTECTED_FIELD")
public class ImobilityCnrSuiteState extends SuiteState {
  public String omobilityId;

  public ImobilityCnrSuiteState(String url, SemanticVersion version) {
    super(url, version);
  }
}
