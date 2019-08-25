package eu.erasmuswithoutpaper.registry.validators.imobilitytorsvalidator.index;

import java.util.Arrays;
import java.util.Collections;

import eu.erasmuswithoutpaper.registry.validators.AbstractValidationSuite;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.Combination;
import eu.erasmuswithoutpaper.registry.validators.ValidatedApiInfo;
import eu.erasmuswithoutpaper.registry.validators.ValidationStepWithStatus.Status;
import eu.erasmuswithoutpaper.registry.validators.imobilitytorsvalidator.IMobilityTorsSuiteState;
import eu.erasmuswithoutpaper.registry.validators.verifiers.VerifierFactory;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Describes the set of test/steps to be run on an IMobility ToRs API index endpoint implementation
 * in order to properly validate it.
 */
class IMobilityTorsIndexValidationSuiteV070
    extends AbstractValidationSuite<IMobilityTorsSuiteState> {
  private static final Logger logger =
      LoggerFactory.getLogger(IMobilityTorsIndexValidationSuiteV070.class);
  private static final ValidatedApiInfo apiInfo = new IMobilityTorsIndexValidatedApiInfo();

  @Override
  protected Logger getLogger() {
    return logger;
  }

  @Override
  public ValidatedApiInfo getApiInfo() {
    return apiInfo;
  }

  IMobilityTorsIndexValidationSuiteV070(ApiValidator<IMobilityTorsSuiteState> validator,
      IMobilityTorsSuiteState state, ValidationSuiteConfig config) {
    super(validator, state, config);
  }

  //FindBugs is not smart enough to infer that actual type of this.currentState
  //is IMobilityTorsSuiteState not just SuiteState
  @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
  protected void validateCombinationAny(Combination combination)
      throws SuiteBroken {
    testParameters200(
        combination,
        "Request one known receiving_hei_id, expect 200 OK.",
        Arrays.asList(
            new Parameter("receiving_hei_id", this.currentState.receivingHeiId)
        ),
        omobilityIdVerifierFactory.expectResponseToBeNotEmpty()
    );

    testParametersError(
        combination,
        "Request with known receiving_hei_id twice, expect 400.",
        Arrays.asList(
            new Parameter("receiving_hei_id", this.currentState.receivingHeiId),
            new Parameter("receiving_hei_id", this.currentState.receivingHeiId)
        ),
        400
    );

    testParametersError(
        combination,
        "Request without receiving_hei_id and known sending_hei_id, expect 400.",
        Arrays.asList(
            new Parameter("sending_hei_id", this.currentState.sendingHeiId)
        ),
        400
    );

    testParameters200(
        combination,
        "Request with known receiving_hei_id and modified_since in the future, expect 200 OK "
            + "and empty response",
        Arrays.asList(
            new Parameter("receiving_hei_id", this.currentState.receivingHeiId),
            new Parameter("modified_since", "2050-02-12T15:19:21+01:00")
        ),
        omobilityIdVerifierFactory.expectResponseToBeEmpty(),
        Status.WARNING
    );

    testParameters200(
        combination,
        "Request with known receiving_hei_id and modified_since far in the past, expect 200 OK "
            + "and non-empty response.",
        Arrays.asList(
            new Parameter("receiving_hei_id", this.currentState.receivingHeiId),
            new Parameter("modified_since", "1950-02-12T15:19:21+01:00")
        ),
        omobilityIdVerifierFactory.expectResponseToBeNotEmpty()
    );


    testParametersError(
        combination,
        "Request with known receiving_hei_id and unknown receiving_hei_id, expect 400.",
        Arrays.asList(
            new Parameter("receiving_hei_id", this.currentState.receivingHeiId),
            new Parameter("receiving_hei_id", fakeId)
        ),
        400
    );


    testParametersError(
        combination,
        "Request without parameters, expect 400.",
        Collections.emptyList(),
        400
    );


    testParametersError(
        combination,
        "Request with multiple modified_since parameters, expect 400.",
        Arrays.asList(
            new Parameter("receiving_hei_id", this.currentState.receivingHeiId),
            new Parameter("modified_since", "1950-02-12T15:19:21+01:00"),
            new Parameter("modified_since", "1950-02-12T15:19:21+01:00")
        ),
        400
    );

    testParameters200(
        combination,
        "Request with known receiving_hei_id and unknown sending_hei_id, "
            + "expect 200 and empty response.",
        Arrays.asList(
            new Parameter("receiving_hei_id", this.currentState.receivingHeiId),
            new Parameter("sending_hei_id", fakeId)
        ),
        omobilityIdVerifierFactory.expectResponseToBeEmpty()
    );


    testParameters200(
        combination,
        "Request with known receiving_hei_id and without sending_hei_id, expect 200 "
            + "and non-empty response.",
        Arrays.asList(
            new Parameter("receiving_hei_id", this.currentState.receivingHeiId)
        ),
        omobilityIdVerifierFactory.expectResponseToBeNotEmpty()
    );

    testParameters200(
        combination,
        "Request with unknown receiving_hei_id, expect 200 and empty response.",
        Arrays.asList(
            new Parameter("receiving_hei_id", fakeId)
        ),
        omobilityIdVerifierFactory.expectResponseToBeEmpty()
    );

    testParameters200(
        combination,
        "Request with known receiving_hei_id and known and unknown "
            + "sending_hei_id, expect 200 and non-empty response.",
        Arrays.asList(
            new Parameter("receiving_hei_id", this.currentState.receivingHeiId),
            new Parameter("sending_hei_id", this.currentState.sendingHeiId),
            new Parameter("sending_hei_id", fakeId)
        ),
        omobilityIdVerifierFactory.expectResponseToBeNotEmpty()
    );

    testParameters200(
        combination,
        "Request with known receiving_hei_id and two unknown sending_hei_id, expect 200 OK "
            + "and empty response.",
        Arrays.asList(
            new Parameter("receiving_hei_id", this.currentState.receivingHeiId),
            new Parameter("sending_hei_id", fakeId),
            new Parameter("sending_hei_id", fakeId)
        ),
        omobilityIdVerifierFactory.expectResponseToBeEmpty()
    );
  }

  private VerifierFactory omobilityIdVerifierFactory = new VerifierFactory(
      Arrays.asList("omobility-id"));
}
