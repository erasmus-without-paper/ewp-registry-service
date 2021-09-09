package eu.erasmuswithoutpaper.registry.validators.omobilitylasvalidator;

import eu.erasmuswithoutpaper.registry.validators.AbstractApiTest;
import eu.erasmuswithoutpaper.registry.validators.SemanticVersion;

public abstract class OMobilityLAsValidatorTestBase extends AbstractApiTest<OMobilityLAsSuiteState> {
  protected static final String omobilitylasIndexUrl =
      "https://university.example.com/omobilitylas/HTTT/index";
  protected static final String omobilitylasGetUrl =
      "https://university.example.com/omobilitylas/HTTT/get";
  protected static final String omobilitylasUpdateUrl =
      "https://university.example.com/omobilitylas/HTTT/update";

  @Override
  protected String getManifestFilename() {
    return "omobilitylas/manifest.xml";
  }

  @Override
  protected SemanticVersion getVersion() {
    return new SemanticVersion(1, 0, 0);
  }

}

