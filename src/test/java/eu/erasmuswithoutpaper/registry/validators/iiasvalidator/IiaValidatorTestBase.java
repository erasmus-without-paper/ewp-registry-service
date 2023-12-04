package eu.erasmuswithoutpaper.registry.validators.iiasvalidator;

import eu.erasmuswithoutpaper.registry.validators.AbstractApiTest;
import eu.erasmuswithoutpaper.registry.validators.iiavalidator.IiaSuiteState;

public abstract class IiaValidatorTestBase extends AbstractApiTest<IiaSuiteState> {

  protected static final String iiaIndexUrl = "https://university.example.com/iias/HTTT/index";
  protected static final String iiaGetUrl = "https://university.example.com/iias/HTTT/get";

}

