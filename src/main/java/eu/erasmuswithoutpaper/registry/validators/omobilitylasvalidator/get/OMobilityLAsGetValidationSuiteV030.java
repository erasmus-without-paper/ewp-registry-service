package eu.erasmuswithoutpaper.registry.validators.omobilitylasvalidator.get;

import java.util.Arrays;

import eu.erasmuswithoutpaper.registry.validators.AbstractValidationSuite;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.Combination;
import eu.erasmuswithoutpaper.registry.validators.ValidatedApiInfo;
import eu.erasmuswithoutpaper.registry.validators.omobilitylasvalidator.OMobilityLAsSuiteState;
import eu.erasmuswithoutpaper.registry.validators.verifiers.CorrectResponseVerifier;
import eu.erasmuswithoutpaper.registry.validators.verifiers.VerifierFactory;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Describes the set of test/steps to be run on an OMobilityLAs API get endpoint implementation
 * in order to properly validate it.
 */
class OMobilityLAsGetValidationSuiteV030
    extends AbstractValidationSuite<OMobilityLAsSuiteState> {

  private static final Logger logger = LoggerFactory
      .getLogger(
          OMobilityLAsGetValidationSuiteV030.class);

  private static final ValidatedApiInfo apiInfo = new OMobilityLAsGetValidatedApiInfo();

  @Override
  protected Logger getLogger() {
    return logger;
  }

  @Override
  public ValidatedApiInfo getApiInfo() {
    return apiInfo;
  }

  OMobilityLAsGetValidationSuiteV030(ApiValidator<OMobilityLAsSuiteState> validator,
      OMobilityLAsSuiteState state, ValidationSuiteConfig config) {
    super(validator, state, config);
  }

  //FindBugs is not smart enough to infer that actual type of this.currentState
  //is OMobilityLAsSuiteState not just SuiteState
  @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
  protected void validateCombinationAny(Combination combination)
      throws SuiteBroken {

    testParameters200(
        combination,
        "Request for one of known omobility_ids, expect 200 OK.",
        new ParameterList(
            new Parameter("sending_hei_id",
                OMobilityLAsGetValidationSuiteV030.this.currentState.sendingHeiId),
            new Parameter(
                "omobility_id",
                OMobilityLAsGetValidationSuiteV030.this.currentState.omobilityId)
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
