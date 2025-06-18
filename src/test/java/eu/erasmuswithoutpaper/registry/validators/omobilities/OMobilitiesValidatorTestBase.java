package eu.erasmuswithoutpaper.registry.validators.omobilities;

import eu.erasmuswithoutpaper.registry.validators.AbstractApiTest;
import eu.erasmuswithoutpaper.registry.validators.omobilitiesvalidator.OMobilitiesSuiteState;

public abstract class OMobilitiesValidatorTestBase extends AbstractApiTest<OMobilitiesSuiteState> {
  protected static final String omobilitiesIndexUrl =
      "https://university.example.com/omobilities/HTTT/index";
  protected static final String omobilitiesGetUrl =
      "https://university.example.com/omobilities/HTTT/get";
  protected static final String omobilitiesUpdateUrl =
      "https://university.example.com/omobilities/HTTT/update";

}

