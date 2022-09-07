package eu.erasmuswithoutpaper.registry.validators.imobilitytorsvalidator;

import eu.erasmuswithoutpaper.registry.validators.AbstractApiTest;
import eu.erasmuswithoutpaper.registry.validators.SemanticVersion;

public abstract class IMobilityTorsValidatorTestBaseV2 extends AbstractApiTest<IMobilityTorsSuiteState> {
  protected static final String omobilityTorsIndexUrl =
      "https://university.example.com/imobilitytors/HTTT/index";
  protected static final String omobilityTorsGetUrl = "https://university.example.com/imobilitytors/HTTT/get";

  @Override
  protected String getManifestFilename() {
    return "imobilitytorsvalidator/manifest-v2.xml";
  }

  @Override
  protected SemanticVersion getVersion() {
    return new SemanticVersion(2, 0, 0);
  }
}

