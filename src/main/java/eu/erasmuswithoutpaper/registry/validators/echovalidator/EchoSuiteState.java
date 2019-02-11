package eu.erasmuswithoutpaper.registry.validators.echovalidator;

import eu.erasmuswithoutpaper.registry.validators.SemanticVersion;
import eu.erasmuswithoutpaper.registry.validators.SuiteState;

public class EchoSuiteState extends SuiteState {
  public EchoSuiteState(String url, SemanticVersion version) {
    super(url, version);
  }
}
