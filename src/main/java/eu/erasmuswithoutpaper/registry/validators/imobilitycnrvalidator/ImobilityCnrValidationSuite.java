package eu.erasmuswithoutpaper.registry.validators.imobilitycnrvalidator;

import eu.erasmuswithoutpaper.registry.validators.AbstractValidationSuite;
import eu.erasmuswithoutpaper.registry.validators.ApiEndpoint;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.Combination;
import eu.erasmuswithoutpaper.registry.validators.ValidatedApiInfo;
import eu.erasmuswithoutpaper.registry.validators.verifiers.CorrectResponseVerifier;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

class ImobilityCnrValidationSuite extends AbstractValidationSuite<ImobilityCnrSuiteState> {

  ImobilityCnrValidationSuite(ApiValidator<ImobilityCnrSuiteState> validator,
      ImobilityCnrSuiteState state, ValidationSuiteConfig config, int version) {
    super(validator, state, config, version);
  }

  @Override
  protected ValidatedApiInfo createApiInfo(int version) {
    return new ImobilityCnrValidatedApiInfo(version, ApiEndpoint.NO_ENDPOINT);
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
