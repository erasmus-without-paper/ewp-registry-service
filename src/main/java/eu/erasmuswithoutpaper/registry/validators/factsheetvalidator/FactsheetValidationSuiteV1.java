package eu.erasmuswithoutpaper.registry.validators.factsheetvalidator;

import java.util.Arrays;
import java.util.Collections;

import eu.erasmuswithoutpaper.registry.validators.AbstractValidationSuite;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.Combination;
import eu.erasmuswithoutpaper.registry.validators.ValidatedApiInfo;
import eu.erasmuswithoutpaper.registry.validators.verifiers.VerifierFactory;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Describes the set of test/steps to be run on an Institutions API implementation in order to
 * properly validate it.
 */
class FactsheetValidationSuiteV1
    extends AbstractValidationSuite<FactsheetSuiteState> {

  private static final Logger logger =
      LoggerFactory.getLogger(FactsheetValidationSuiteV1.class);

  private static final ValidatedApiInfo apiInfo = new FactsheetValidatedApiInfoV1();

  FactsheetValidationSuiteV1(ApiValidator<FactsheetSuiteState> validator,
      FactsheetSuiteState state, ValidationSuiteConfig config) {
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
  //is FactsheetSuiteState not just SuiteState
  @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
  protected void validateCombinationAny(Combination combination)
      throws SuiteBroken {
    String fakeHeiId = fakeId;
    final VerifierFactory verfierFactory =
        new VerifierFactory(Arrays.asList("factsheet", "hei-id"));


    testParameters200(combination, "Request for one of known hei_ids, expect 200 OK.",
        new ParameterList(new Parameter("hei_id", currentState.selectedHeiId)),
        verfierFactory.expectResponseToContainExactly(
            Collections.singletonList(currentState.selectedHeiId)
        )
    );

    testParameters200(combination, "Request one unknown hei_id, expect 200 and empty response.",
        new ParameterList(new Parameter("hei_id", fakeHeiId)),
        verfierFactory.expectResponseToBeEmpty()
    );

    if (this.currentState.maxHeiIds > 1) {
      testParameters200(
          combination,
          "Request one known and one unknown hei_id, expect 200 and only one <hei-id> in response.",
          new ParameterList(new Parameter("hei_id", currentState.selectedHeiId),
              new Parameter("hei_id", fakeHeiId)
          ),
          verfierFactory.expectResponseToContainExactly(
              Collections.singletonList(currentState.selectedHeiId)
          )
      );
    }

    testParametersError(combination,
        "Request without hei_ids, expect 400.",
        new ParameterList(),
        400
    );

    testParametersError(combination, "Request more than <max-hei-ids> known hei_ids, expect 400.",
        new ParameterList(
            Collections.nCopies(this.currentState.maxHeiIds + 1,
                new Parameter("hei_id", currentState.selectedHeiId)
            )
        ),
        400
    );

    testParametersError(combination,
        "Request more than <max-hei-ids> unknown hei_ids, expect 400.",
        new ParameterList(
            Collections.nCopies(this.currentState.maxHeiIds + 1, new Parameter("hei_id", fakeHeiId))
        ),
        400
    );

    testParameters200(combination, "Request exactly <max-hei-ids> known hei_ids, "
            + "expect 200 and non-empty response.",
        new ParameterList(
            Collections.nCopies(this.currentState.maxHeiIds,
                new Parameter("hei_id", currentState.selectedHeiId)
            )
        ),
        verfierFactory
            .expectResponseToContain(Collections.singletonList(currentState.selectedHeiId))
    );

    testParametersError(combination, "Request with single incorrect parameter, expect 400.",
        new ParameterList(new Parameter("hei_id_param", currentState.selectedHeiId)), 400
    );
  }
}
