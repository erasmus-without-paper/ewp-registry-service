package eu.erasmuswithoutpaper.registry.validators.omobilitiesvalidator.get;

import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.Combination;
import eu.erasmuswithoutpaper.registry.validators.omobilitiesvalidator.OMobilitiesSuiteState;
import eu.erasmuswithoutpaper.registry.validators.verifiers.CorrectResponseVerifier;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class OMobilitiesGetValidationSuiteV3 extends OMobilitiesGetValidationSuiteV2 {
  OMobilitiesGetValidationSuiteV3(ApiValidator<OMobilitiesSuiteState> validator,
      OMobilitiesSuiteState state, ValidationSuiteConfig config, int version) {
    super(validator, state, config, version);
  }

  @Override
  //FindBugs is not smart enough to infer that actual type of this.currentState
  //is OMobilitiesSuiteState not just SuiteState
  @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
  protected void validateCombinationAny(Combination combination)
          throws SuiteBroken {

    testParameters200(
        combination, "Request for one of known omobility_ids, expect 200 OK.",
        getParameterList(),
        new CorrectResponseVerifier()
    );

    generalTestsIds(combination,
        null, null,
        "omobility",
        this.currentState.omobilityId, this.currentState.maxOmobilityIds,
        true,
        omobilityIdVerifierFactory
    );
  }

  @Override
  @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
  protected ParameterList getParameterList() {
    return new ParameterList(new Parameter("omobility_id", currentState.omobilityId));
  }
}
