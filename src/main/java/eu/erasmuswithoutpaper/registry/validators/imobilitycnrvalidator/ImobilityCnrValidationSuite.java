package eu.erasmuswithoutpaper.registry.validators.imobilitycnrvalidator;

import eu.erasmuswithoutpaper.registry.validators.AbstractValidationSuite;
import eu.erasmuswithoutpaper.registry.validators.ApiEndpoint;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.Combination;
import eu.erasmuswithoutpaper.registry.validators.ValidatedApiInfo;
import eu.erasmuswithoutpaper.registry.validators.verifiers.CorrectResponseVerifier;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ImobilityCnrValidationSuite extends AbstractValidationSuite<ImobilityCnrSuiteState> {

  private static final Logger logger = LoggerFactory.getLogger(ImobilityCnrValidationSuite.class);

  private final ValidatedApiInfo apiInfo;

  ImobilityCnrValidationSuite(ApiValidator<ImobilityCnrSuiteState> validator,
      ImobilityCnrSuiteState state, ValidationSuiteConfig config, int version) {
    super(validator, state, config);

    this.apiInfo = new ImobilityCnrValidatedApiInfo(version, ApiEndpoint.NO_ENDPOINT);
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
  @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
  protected void validateCombinationAny(Combination combination) throws SuiteBroken {
    testParameters200(combination, "Correct request, expect 200 and empty response.",
        new ParameterList(new Parameter("omobility_id", currentState.omobilityId)),
        new CorrectResponseVerifier());

    testParametersError(combination, "Request without omobility_id, expect 400.",
        new ParameterList(), 400);
  }
}
