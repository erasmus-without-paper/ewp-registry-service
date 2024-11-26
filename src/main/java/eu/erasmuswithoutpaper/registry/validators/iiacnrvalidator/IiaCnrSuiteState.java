package eu.erasmuswithoutpaper.registry.validators.iiacnrvalidator;

import eu.erasmuswithoutpaper.registry.validators.SemanticVersion;
import eu.erasmuswithoutpaper.registry.validators.SuiteState;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@SuppressFBWarnings("UUF_UNUSED_PUBLIC_OR_PROTECTED_FIELD")
public class IiaCnrSuiteState extends SuiteState {
  public String iiaId;

  public IiaCnrSuiteState(String url, SemanticVersion version) {
    super(url, version);
  }
}
