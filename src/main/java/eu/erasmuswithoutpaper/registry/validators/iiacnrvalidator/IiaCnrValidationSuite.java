package eu.erasmuswithoutpaper.registry.validators.iiacnrvalidator;

import eu.erasmuswithoutpaper.registry.validators.AbstractValidationSuite;
import eu.erasmuswithoutpaper.registry.validators.ApiEndpoint;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.Combination;
import eu.erasmuswithoutpaper.registry.validators.SuiteState;
import eu.erasmuswithoutpaper.registry.validators.ValidatedApiInfo;
import eu.erasmuswithoutpaper.registry.validators.verifiers.CorrectResponseVerifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class IiaCnrValidationSuite
    extends AbstractValidationSuite<SuiteState> {

  private static final Logger logger =
      LoggerFactory.getLogger(IiaCnrValidationSuite.class);
  private static final String FAKE_IIA_ID = "1";

  private final ValidatedApiInfo apiInfo;

  IiaCnrValidationSuite(ApiValidator<SuiteState> validator, SuiteState state,
      ValidationSuiteConfig config, int version) {
    super(validator, state, config);

    this.apiInfo = new IiaCnrValidatedApiInfo(version, ApiEndpoint.NO_ENDPOINT);
  }

  @Override
  protected Logger getLogger() {
    return logger;
  }

  @Override
  public ValidatedApiInfo getApiInfo() {
    return apiInfo;
  }

  @Override
  protected void validateCombinationAny(Combination combination)
      throws SuiteBroken {
    testParameters200(combination, "Correct request, expect 200 and empty response.",
        new ParameterList(new Parameter("iia_id", FAKE_IIA_ID)),
        new CorrectResponseVerifier()
    );

    testParametersError(combination,
        "Request without iia_id, expect 400.",
        new ParameterList(),
        400
    );
  }
}
