package eu.erasmuswithoutpaper.registry.validators.iiacnrvalidator;

import eu.erasmuswithoutpaper.registry.validators.AbstractValidationSuite;
import eu.erasmuswithoutpaper.registry.validators.ApiEndpoint;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.Combination;
import eu.erasmuswithoutpaper.registry.validators.ValidatedApiInfo;
import eu.erasmuswithoutpaper.registry.validators.verifiers.CorrectResponseVerifier;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

class IiaCnrValidationSuite
    extends AbstractValidationSuite<IiaCnrSuiteState> {

  IiaCnrValidationSuite(ApiValidator<IiaCnrSuiteState> validator, IiaCnrSuiteState state,
      ValidationSuiteConfig config, int version) {
    super(validator, state, config, version);
  }

  @Override
  protected ValidatedApiInfo createApiInfo(int version) {
    return new IiaCnrValidatedApiInfo(version, ApiEndpoint.NO_ENDPOINT);
  }

  @Override
  @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
  protected void validateCombinationAny(Combination combination)
      throws SuiteBroken {
    testParameters200(combination, "Correct request, expect 200 and empty response.",
        new ParameterList(new Parameter("iia_id", currentState.iiaId)),
        new CorrectResponseVerifier()
    );

    testParametersError(combination,
        "Request without iia_id, expect 400.",
        new ParameterList(),
        400
    );
  }
}
