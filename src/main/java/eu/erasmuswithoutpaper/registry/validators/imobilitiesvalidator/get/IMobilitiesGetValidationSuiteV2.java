package eu.erasmuswithoutpaper.registry.validators.imobilitiesvalidator.get;

import java.util.Arrays;

import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.Combination;
import eu.erasmuswithoutpaper.registry.validators.ValidationStepWithStatus;
import eu.erasmuswithoutpaper.registry.validators.imobilitiesvalidator.IMobilitiesSuiteState;
import eu.erasmuswithoutpaper.registry.validators.verifiers.CorrectResponseVerifier;
import eu.erasmuswithoutpaper.registry.validators.verifiers.VerifierFactory;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

class IMobilitiesGetValidationSuiteV2 extends IMobilitiesGetValidationSuiteV1 {

  private final VerifierFactory omobilityIdVerifierFactory =
      new VerifierFactory(Arrays.asList("student-mobility", "omobility-id"));

  IMobilitiesGetValidationSuiteV2(ApiValidator<IMobilitiesSuiteState> validator,
      IMobilitiesSuiteState state, ValidationSuiteConfig config, int version) {
    super(validator, state, config, version);
  }

  @Override
  // FindBugs is not smart enough to infer that actual type of this.currentState
  // is IMobilitiesSuiteState not just SuiteState
  @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
  protected void validateCombinationAny(Combination combination) throws SuiteBroken {
    testParameters200(combination, "Request for one of known omobility_ids, expect 200 OK.",
        new ParameterList(new Parameter("omobility_id", currentState.omobilityId)),
        new CorrectResponseVerifier());

    generalTestsIds(combination, null, null, "omobility", this.currentState.omobilityId,
        this.currentState.maxOmobilityIds, true, omobilityIdVerifierFactory);

    // Are others able to see imobilities visible to me?
    testParameters200AsOtherEwpParticipant(combination,
        "Request omobility_id as other EWP participant, expect 200 OK and empty response.",
        new ParameterList(new Parameter("omobility_id", this.currentState.omobilityId)),
        omobilityIdVerifierFactory.expectResponseToBeEmpty(),
        ValidationStepWithStatus.Status.WARNING);
  }
}
