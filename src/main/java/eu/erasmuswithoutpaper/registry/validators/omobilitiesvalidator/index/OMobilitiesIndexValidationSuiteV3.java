package eu.erasmuswithoutpaper.registry.validators.omobilitiesvalidator.index;

import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.Combination;
import eu.erasmuswithoutpaper.registry.validators.ValidationStepWithStatus;
import eu.erasmuswithoutpaper.registry.validators.omobilitiesvalidator.OMobilitiesSuiteState;
import eu.erasmuswithoutpaper.registry.validators.verifiers.Verifier;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class OMobilitiesIndexValidationSuiteV3 extends OMobilitiesIndexValidationSuiteV2 {
  OMobilitiesIndexValidationSuiteV3(ApiValidator<OMobilitiesSuiteState> validator,
      OMobilitiesSuiteState state, ValidationSuiteConfig config, int version) {
    super(validator, state, config, version);
  }

  @Override
  //FindBugs is not smart enough to infer that actual type of this.currentState
  //is OMobilitiesSuiteState not just SuiteState
  @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
  protected void validateCombinationAny(Combination combination)
          throws SuiteBroken {
    Verifier hasAnyElementVerifier = omobilityIdVerifierFactory.expectResponseToBeNotEmpty();
    hasAnyElementVerifier.setCustomErrorMessage("However we received an empty response, no"
        + " omobility ids were returned. Some tests will be skipped. To perform more tests"
        + " provide parameters that will allow us to receive omobility-ids.");
    testParameters200(
        combination,
        "Request without parameters, expect 200 OK.",
        new ParameterList(),
        hasAnyElementVerifier,
        ValidationStepWithStatus.Status.FAILURE
    );

    final boolean noOMobilityIdReturned = !hasAnyElementVerifier.getVerificationResult();
    final String noOMobilitySkipReason = "OMobilities list was empty.";

    testReceivingAcademicYears(combination, null, null, omobilityIdVerifierFactory);

    // Modified since
    modifiedSinceTests(combination, null, null, true, noOMobilityIdReturned, noOMobilitySkipReason,
        omobilityIdVerifierFactory);
  }
}
