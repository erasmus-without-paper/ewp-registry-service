package eu.erasmuswithoutpaper.registry.validators.omobilitycnrvalidator;

import eu.erasmuswithoutpaper.registry.validators.SemanticVersion;
import eu.erasmuswithoutpaper.registry.validators.SuiteState;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@SuppressFBWarnings("UUF_UNUSED_PUBLIC_OR_PROTECTED_FIELD")
public class OmobilityCnrSuiteState extends SuiteState {
  public String omobilityId;

  public OmobilityCnrSuiteState(String url, SemanticVersion version) {
    super(url, version);
  }
}
