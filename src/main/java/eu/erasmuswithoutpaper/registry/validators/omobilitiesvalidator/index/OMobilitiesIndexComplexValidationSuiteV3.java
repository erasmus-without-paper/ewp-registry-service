package eu.erasmuswithoutpaper.registry.validators.omobilitiesvalidator.index;

import java.util.Collections;

import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.Combination;
import eu.erasmuswithoutpaper.registry.validators.omobilitiesvalidator.OMobilitiesSuiteState;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class OMobilitiesIndexComplexValidationSuiteV3
    extends OMobilitiesIndexComplexValidationSuiteV2 {
  OMobilitiesIndexComplexValidationSuiteV3(ApiValidator<OMobilitiesSuiteState> validator,
      OMobilitiesSuiteState state, ValidationSuiteConfig config, int version) {
    super(validator, state, config, version);
  }

  @Override
  // FindBugs is not smart enough to infer that actual type of this.currentState
  // is OMobilitiesSuiteState not just SuiteState
  @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
  protected void validateCombinationAny(Combination combination) throws SuiteBroken {
    testParameters200(combination,
        "Request without parameters, expect 200 OK and a specific omobility in response.",
        new ParameterList(), omobilityIdVerifierFactory
            .expectResponseToContain(Collections.singletonList(this.currentState.omobilityId)));

    testReceivingAcademicYearsReturnsExpectedId(combination, null, null, omobilityIdVerifierFactory,
        this.currentState.receivingAcademicYearId, this.currentState.omobilityId);

    testModifiedSinceReturnsSpecifiedId(combination, null, null, true,
        this.currentState.omobilityId == null, "No omobility-id found.", omobilityIdVerifierFactory,
        this.currentState.omobilityId);
  }
}
