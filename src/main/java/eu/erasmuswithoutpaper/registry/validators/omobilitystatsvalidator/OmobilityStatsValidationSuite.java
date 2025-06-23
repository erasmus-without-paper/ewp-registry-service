package eu.erasmuswithoutpaper.registry.validators.omobilitystatsvalidator;

import java.util.List;

import eu.erasmuswithoutpaper.registry.validators.AbstractValidationSuite;
import eu.erasmuswithoutpaper.registry.validators.ApiEndpoint;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.Combination;
import eu.erasmuswithoutpaper.registry.validators.SuiteState;
import eu.erasmuswithoutpaper.registry.validators.ValidatedApiInfo;
import eu.erasmuswithoutpaper.registry.validators.verifiers.VerifierFactory;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

class OmobilityStatsValidationSuite extends AbstractValidationSuite<SuiteState> {

  private final VerifierFactory statsVerifierFactory =
      new VerifierFactory(List.of("academic-year-stats"));

  OmobilityStatsValidationSuite(ApiValidator<SuiteState> validator,
      SuiteState state, ValidationSuiteConfig config, int version) {
    super(validator, state, config, version);
  }

  @Override
  protected ValidatedApiInfo createApiInfo(int version) {
    return new OmobilityStatsValidatedApiInfo(version, ApiEndpoint.NO_ENDPOINT);
  }

  @Override
  @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
  protected void validateCombinationAny(Combination combination) throws SuiteBroken {
    testParameters200(combination, "Correct request, expect 200 and non-empty response.",
        new ParameterList(), statsVerifierFactory.expectResponseToBeNotEmpty());
  }
}
