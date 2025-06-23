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

/**
 * Describes the set of test/steps to be run on an OMobilities API get endpoint implementation
 * in order to properly validate it.
 */
class OMobilitiesGetValidationSuiteV2
    extends AbstractValidationSuite<OMobilitiesSuiteState> {

  private final ValidatedApiInfo apiInfo;

  @Override
  public ValidatedApiInfo getApiInfo() {
    return apiInfo;
  }

  OMobilitiesGetValidationSuiteV2(ApiValidator<OMobilitiesSuiteState> validator,
      OMobilitiesSuiteState state, ValidationSuiteConfig config, int version) {
    super(validator, state, config);

    this.apiInfo = new OMobilitiesValidatedApiInfo(version, ApiEndpoint.GET);
  }

  @Override
  //FindBugs is not smart enough to infer that actual type of this.currentState
  //is OMobilitiesSuiteState not just SuiteState
  @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
  protected void validateCombinationAny(Combination combination)
      throws SuiteBroken {

    testParameters200(
        combination, "Request for one of known omobility_ids, expect 200 OK.", getParameterList(),
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

  @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
  protected ParameterList getParameterList() {
    return new ParameterList(
        new Parameter("sending_hei_id", currentState.sendingHeiId),
        new Parameter("omobility_id", currentState.omobilityId)
    );
  }

  protected final VerifierFactory omobilityIdVerifierFactory =
      new VerifierFactory(Arrays.asList("student-mobility", "omobility-id"));
}
