package eu.erasmuswithoutpaper.registry.validators.omobilitylasvalidator.get;

import java.util.Arrays;

import eu.erasmuswithoutpaper.registry.validators.AbstractValidationSuite;
import eu.erasmuswithoutpaper.registry.validators.ApiEndpoint;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.Combination;
import eu.erasmuswithoutpaper.registry.validators.ValidatedApiInfo;
import eu.erasmuswithoutpaper.registry.validators.omobilitylasvalidator.OMobilityLAsGetValidatedApiInfo;
import eu.erasmuswithoutpaper.registry.validators.omobilitylasvalidator.OMobilityLAsSuiteState;
import eu.erasmuswithoutpaper.registry.validators.verifiers.CorrectResponseVerifier;
import eu.erasmuswithoutpaper.registry.validators.verifiers.VerifierFactory;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Describes the set of test/steps to be run on an OMobilityLAs API get endpoint implementation
 * in order to properly validate it.
 */
class OMobilityLAsGetValidationSuite
    extends AbstractValidationSuite<OMobilityLAsSuiteState> {

  @Override
  protected ValidatedApiInfo createApiInfo(int version) {
    return new OMobilityLAsGetValidatedApiInfo(version, ApiEndpoint.GET);
  }

  OMobilityLAsGetValidationSuite(ApiValidator<OMobilityLAsSuiteState> validator,
                                   OMobilityLAsSuiteState state, ValidationSuiteConfig config,
                                   int version) {
    super(validator, state, config, version);
  }

  @Override
  //FindBugs is not smart enough to infer that actual type of this.currentState
  //is OMobilityLAsSuiteState not just SuiteState
  @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
  protected void validateCombinationAny(Combination combination)
      throws SuiteBroken {

    testParameters200(
        combination,
        "Request for one of known omobility_ids, expect 200 OK.",
        new ParameterList(
            new Parameter("sending_hei_id", currentState.sendingHeiId),
            new Parameter("omobility_id", currentState.omobilityId)
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
      new VerifierFactory(Arrays.asList("la", "omobility-id"));
}
