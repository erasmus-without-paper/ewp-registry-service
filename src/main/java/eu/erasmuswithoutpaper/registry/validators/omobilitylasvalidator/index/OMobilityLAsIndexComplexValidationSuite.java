package eu.erasmuswithoutpaper.registry.validators.omobilitylasvalidator.index;

import java.util.Arrays;

import eu.erasmuswithoutpaper.registry.validators.AbstractValidationSuite;
import eu.erasmuswithoutpaper.registry.validators.ApiEndpoint;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.Combination;
import eu.erasmuswithoutpaper.registry.validators.ValidatedApiInfo;
import eu.erasmuswithoutpaper.registry.validators.omobilitylasvalidator.OMobilityLAsGetValidatedApiInfo;
import eu.erasmuswithoutpaper.registry.validators.omobilitylasvalidator.OMobilityLAsSuiteState;
import eu.erasmuswithoutpaper.registry.validators.verifiers.VerifierFactory;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Describes the set of test/steps to be run on an OMobilities API index endpoint implementation
 * in order to properly validate it.
 */
public class OMobilityLAsIndexComplexValidationSuite
    extends AbstractValidationSuite<OMobilityLAsSuiteState> {

  @Override
  protected ValidatedApiInfo createApiInfo(int version) {
    return new OMobilityLAsGetValidatedApiInfo(version, ApiEndpoint.INDEX);
  }

  OMobilityLAsIndexComplexValidationSuite(ApiValidator<OMobilityLAsSuiteState> validator,
      OMobilityLAsSuiteState state, ValidationSuiteConfig config, int version) {
    super(validator, state, config, version);
  }

  @Override
  //FindBugs is not smart enough to infer that actual type of this.currentState
  //is OMobilitiesSuiteState not just SuiteState
  @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
  protected void validateCombinationAny(Combination combination)
      throws SuiteBroken {
    testParameters200(
        combination,
        "Request known sending_hei_id, expect 200 OK and "
            + "specific omobility in response.",
        new ParameterList(
            new Parameter("sending_hei_id", this.currentState.sendingHeiId)
        ),
        omobilityIdVerifierFactory.expectResponseToContain(Arrays.asList(
            this.currentState.omobilityId
        ))
    );

    testReceivingAcademicYearsReturnsExpectedId(combination,
        "sending_hei_id",
        this.currentState.sendingHeiId,
        omobilityIdVerifierFactory,
        this.currentState.receivingAcademicYearId,
        this.currentState.omobilityId
    );

    testModifiedSinceReturnsSpecifiedId(combination,
        "sending_hei_id",
        this.currentState.sendingHeiId,
        true,
        this.currentState.omobilityId == null,
        "No omobility-id found.",
        omobilityIdVerifierFactory,
        this.currentState.omobilityId
    );
  }

  private VerifierFactory omobilityIdVerifierFactory = new VerifierFactory(
      Arrays.asList("omobility-id"));
}
