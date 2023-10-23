package eu.erasmuswithoutpaper.registry.validators.omobilitiesvalidator.index;

import java.util.Arrays;

import eu.erasmuswithoutpaper.registry.validators.AbstractValidationSuite;
import eu.erasmuswithoutpaper.registry.validators.ApiEndpoint;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.Combination;
import eu.erasmuswithoutpaper.registry.validators.ValidatedApiInfo;
import eu.erasmuswithoutpaper.registry.validators.omobilitiesvalidator.OMobilitiesSuiteState;
import eu.erasmuswithoutpaper.registry.validators.omobilitiesvalidator.OMobilitiesValidatedApiInfo;
import eu.erasmuswithoutpaper.registry.validators.verifiers.VerifierFactory;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Describes the set of test/steps to be run on an OMobilities API index endpoint implementation
 * in order to properly validate it.
 */
public class OMobilitiesIndexComplexValidationSuite
    extends AbstractValidationSuite<OMobilitiesSuiteState> {

  private static final Logger logger =
      LoggerFactory.getLogger(
          OMobilitiesIndexComplexValidationSuite.class);

  private final ValidatedApiInfo apiInfo;

  @Override
  protected Logger getLogger() {
    return logger;
  }

  @Override
  public ValidatedApiInfo getApiInfo() {
    return apiInfo;
  }

  OMobilitiesIndexComplexValidationSuite(ApiValidator<OMobilitiesSuiteState> validator,
      OMobilitiesSuiteState state, ValidationSuiteConfig config, int version) {
    super(validator, state, config);

    this.apiInfo = new OMobilitiesValidatedApiInfo(version, ApiEndpoint.INDEX);
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
