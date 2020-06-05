package eu.erasmuswithoutpaper.registry.validators.imobilitiesvalidator.get;

import java.util.Arrays;

import eu.erasmuswithoutpaper.registry.validators.AbstractValidationSuite;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.Combination;
import eu.erasmuswithoutpaper.registry.validators.ValidatedApiInfo;
import eu.erasmuswithoutpaper.registry.validators.ValidationStepWithStatus;
import eu.erasmuswithoutpaper.registry.validators.imobilitiesvalidator.IMobilitiesSuiteState;
import eu.erasmuswithoutpaper.registry.validators.verifiers.CorrectResponseVerifier;
import eu.erasmuswithoutpaper.registry.validators.verifiers.VerifierFactory;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Describes the set of test/steps to be run on an IMobilities API get endpoint implementation
 * in order to properly validate it.
 */
class IMobilitiesGetValidationSuiteV1
    extends AbstractValidationSuite<IMobilitiesSuiteState> {

  private static final Logger logger = LoggerFactory
      .getLogger(
          IMobilitiesGetValidationSuiteV1.class);

  private static final ValidatedApiInfo apiInfo = new IMobilitiesGetValidatedApiInfo();
  private VerifierFactory omobilityIdVerifierFactory =
      new VerifierFactory(Arrays.asList("student-mobility-for-studies", "omobility-id"));

  IMobilitiesGetValidationSuiteV1(ApiValidator<IMobilitiesSuiteState> validator,
      IMobilitiesSuiteState state, ValidationSuiteConfig config) {
    super(validator, state, config);
  }

  @Override
  protected Logger getLogger() {
    return logger;
  }

  @Override
  public ValidatedApiInfo getApiInfo() {
    return apiInfo;
  }

  //FindBugs is not smart enough to infer that actual type of this.currentState
  //is IMobilitiesSuiteState not just SuiteState
  @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
  protected void validateCombinationAny(Combination combination)
      throws SuiteBroken {

    testParameters200(
        combination,
        "Request for one of known omobility_ids, expect 200 OK.",
        Arrays.asList(
            new Parameter("receiving_hei_id",
                IMobilitiesGetValidationSuiteV1.this.currentState.receivingHeiId),
            new Parameter(
                "omobility_id",
                IMobilitiesGetValidationSuiteV1.this.currentState.omobilityId)
        ),
        new CorrectResponseVerifier()
    );

    generalTestsIds(combination,
        "receiving_hei_id", this.currentState.receivingHeiId,
        "omobility",
        this.currentState.omobilityId, this.currentState.maxOmobilityIds,
        true,
        omobilityIdVerifierFactory
    );

    testParametersError(
        combination,
        "Request with correct receiving_hei_id and incorrect receiving_hei_id, expect 400.",
        Arrays.asList(
            new Parameter("receiving_hei_id", this.currentState.receivingHeiId),
            new Parameter("receiving_hei_id", fakeId),
            new Parameter("omobility_id",
                IMobilitiesGetValidationSuiteV1.this.currentState.omobilityId)
        ),
        400
    );

    // Are others able to see imobilities visible to me?
    testParameters200AsOtherEwpParticipant(
        combination,
        "Request one known receiving_hei_id and omobility_id as other EWP participant, expect 200"
            + " OK and empty response.",
        Arrays.asList(
            new Parameter("receiving_hei_id", this.currentState.receivingHeiId),
            new Parameter("omobility_id", this.currentState.omobilityId)
        ),
        omobilityIdVerifierFactory.expectResponseToBeEmpty(),
        ValidationStepWithStatus.Status.WARNING
    );

  }
}
