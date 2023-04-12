package eu.erasmuswithoutpaper.registry.validators.omobilities;

import eu.erasmuswithoutpaper.registry.validators.AbstractApiTest;
import eu.erasmuswithoutpaper.registry.validators.SemanticVersion;
import eu.erasmuswithoutpaper.registry.validators.omobilitiesvalidator.OMobilitiesSuiteState;

public abstract class OMobilitiesValidatorTestBase extends AbstractApiTest<OMobilitiesSuiteState> {
  protected static final String omobilitiesIndexUrl =
      "https://university.example.com/omobilities/HTTT/index";
  protected static final String omobilitiesGetUrl =
      "https://university.example.com/omobilities/HTTT/get";

  @Override
  protected String getManifestFilename() {
    return "omobilities/manifest.xml";
  }

  @Override
  protected SemanticVersion getVersion() {
    return new SemanticVersion(2, 0, 0);
  }
}

