package eu.erasmuswithoutpaper.registry.validators.omobilitiesvalidator.get;

import java.util.Arrays;

import eu.erasmuswithoutpaper.registry.validators.AbstractValidationSuite;
import eu.erasmuswithoutpaper.registry.validators.ApiEndpoint;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.Combination;
import eu.erasmuswithoutpaper.registry.validators.ValidatedApiInfo;
import eu.erasmuswithoutpaper.registry.validators.omobilitiesvalidator.OMobilitiesSuiteState;
import eu.erasmuswithoutpaper.registry.validators.omobilitiesvalidator.OMobilitiesValidatedApiInfo;
import eu.erasmuswithoutpaper.registry.validators.verifiers.CorrectResponseVerifier;
import eu.erasmuswithoutpaper.registry.validators.verifiers.VerifierFactory;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Describes the set of test/steps to be run on an OMobilities API get endpoint implementation
 * in order to properly validate it.
 */
class OMobilitiesGetValidationSuite
    extends AbstractValidationSuite<OMobilitiesSuiteState> {

  private static final Logger logger = LoggerFactory
      .getLogger(
          OMobilitiesGetValidationSuite.class);

  private final ValidatedApiInfo apiInfo;

  @Override
  protected Logger getLogger() {
    return logger;
  }

  @Override
  public ValidatedApiInfo getApiInfo() {
    return apiInfo;
  }

  OMobilitiesGetValidationSuite(ApiValidator<OMobilitiesSuiteState> validator,
      OMobilitiesSuiteState state, ValidationSuiteConfig config, int version) {
    super(validator, state, config);

    this.apiInfo = new OMobilitiesValidatedApiInfo(version, ApiEndpoint.Get);
  }

  //FindBugs is not smart enough to infer that actual type of this.currentState
  //is OMobilitiesSuiteState not just SuiteState
  @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
  protected void validateCombinationAny(Combination combination)
      throws SuiteBroken {

    testParameters200(
        combination,
        "Request for one of known omobility_ids, expect 200 OK.",
        new ParameterList(
            new Parameter("sending_hei_id",
                OMobilitiesGetValidationSuite.this.currentState.sendingHeiId),
            new Parameter(
                "omobility_id",
                OMobilitiesGetValidationSuite.this.currentState.omobilityId)
        ),
        new CorrectResponseVerifier()
    );

    generalTestsIds(combination,
        "sending_hei_id", this.currentState.sendingHeiId,
        "omobility",
        this.currentState.omobilityId, this.currentState.maxOmobilityIds,
        true,
        omobilityIdVerifierFactory
    );
  }

  private VerifierFactory omobilityIdVerifierFactory =
      new VerifierFactory(Arrays.asList("student-mobility-for-studies", "omobility-id"));
}
