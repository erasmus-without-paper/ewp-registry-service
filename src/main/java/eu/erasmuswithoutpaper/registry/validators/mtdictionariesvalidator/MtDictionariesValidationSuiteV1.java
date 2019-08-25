package eu.erasmuswithoutpaper.registry.validators.mtdictionariesvalidator;

import java.util.ArrayList;
import java.util.Arrays;

import eu.erasmuswithoutpaper.registry.validators.AbstractValidationSuite;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.Combination;
import eu.erasmuswithoutpaper.registry.validators.ValidatedApiInfo;
import eu.erasmuswithoutpaper.registry.validators.verifiers.CorrectResponseVerifier;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Describes the set of test/steps to be run on an MT Dictionaries API implementation in order to
 * properly validate it.
 */
class MtDictionariesValidationSuiteV1 extends AbstractValidationSuite<MtDictionariesSuiteState> {

  private static final Logger logger =
      LoggerFactory.getLogger(MtDictionariesValidationSuiteV1.class);

  private static final ValidatedApiInfo apiInfo = new MtDictionariesValidatedApiInfoV1();

  @Override
  protected Logger getLogger() {
    return logger;
  }

  @Override
  public ValidatedApiInfo getApiInfo() {
    return apiInfo;
  }

  MtDictionariesValidationSuiteV1(ApiValidator<MtDictionariesSuiteState> validator,
      MtDictionariesSuiteState state, ValidationSuiteConfig config) {
    super(validator, state, config);
  }

  //FindBugs is not smart enough to infer that actual type of this.currentState
  //is MtDictionariesSuiteState not just SuiteState
  @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
  protected void validateCombinationAny(Combination combination)
      throws SuiteBroken {
    testParameters200(
        combination,
        "Request with known dictionary and call_year, expect 200 and non empty response.",
        Arrays.asList(
            new Parameter("dictionary", this.currentState.selectedDictionary),
            new Parameter("call_year", this.currentState.selectedCallYear)
        ),
        new CorrectResponseVerifier()
    );

    testParameters200(
        combination,
        "Request with known dictionary, call_year and invalid parameter, expect 200.",
        Arrays.asList(
            new Parameter("dictionary", this.currentState.selectedDictionary),
            new Parameter("call_year", this.currentState.selectedCallYear),
            new Parameter("dictionary_param", this.currentState.selectedDictionary)
        ),
        new CorrectResponseVerifier()
    );

    testParametersError(
        combination,
        "Request with correct dictionary twice, expect 400.",
        Arrays.asList(
            new Parameter("dictionary", this.currentState.selectedDictionary),
            new Parameter("dictionary", this.currentState.selectedDictionary),
            new Parameter("call_year", this.currentState.selectedCallYear)
        ),
        400
    );

    testParametersError(
        combination,
        "Request with correct call_year twice, expect 400.",
        Arrays.asList(
            new Parameter("dictionary", this.currentState.selectedDictionary),
            new Parameter("call_year", this.currentState.selectedCallYear),
            new Parameter("call_year", this.currentState.selectedCallYear)
        ),
        400
    );

    testParametersError(
        combination,
        "Request with single incorrect parameter, expect 400.",
        Arrays.asList(new Parameter("dictionary_param", fakeId)),
        400
    );

    testParametersError(
        combination,
        "Request without dictionary, expect 400.",
        Arrays.asList(
            new Parameter("call_year", this.currentState.selectedCallYear)
        ),
        400
    );

    testParametersError(
        combination,
        "Request without call_year, expect 400.",
        Arrays.asList(
            new Parameter("dictionary", this.currentState.selectedDictionary)
        ),
        400
    );

    testParametersError(
        combination,
        "Request without any parameter, expect 400.",
        new ArrayList<>(),
        400
    );

    testParametersError(
        combination,
        "Request with unknown dictionary parameter, expect 400.",
        Arrays.asList(
            new Parameter("dictionary", fakeId),
            new Parameter("call_year", this.currentState.selectedCallYear)
        ),
        400
    );

    testParametersError(
        combination,
        "Request with invalid value of call_year - not a number, expect 400.",
        Arrays.asList(
            new Parameter("dictionary", this.currentState.selectedDictionary),
            new Parameter("call_year", "not-a-number")
        ),
        400
    );

    testParameters200(
        combination,
        "Request with call_year equal zero, expect 200.",
        Arrays.asList(
            new Parameter("dictionary", this.currentState.selectedDictionary),
            new Parameter("call_year", "0")
        ),
        new CorrectResponseVerifier()
    );

    testParameters200(
        combination,
        "Request with negative call_year, expect 200.",
        Arrays.asList(
            new Parameter("dictionary", this.currentState.selectedDictionary),
            new Parameter("call_year", "-2019")
        ),
        new CorrectResponseVerifier()
    );

    testParametersError(
        combination,
        "Request with call_year being a date, expect 400.",
        Arrays.asList(
            new Parameter("dictionary", this.currentState.selectedDictionary),
            new Parameter("call_year", "2004-02-12")
        ),
        400
    );

    testParametersError(
        combination,
        "Request with call_year being a date and time, expect 400.",
        Arrays.asList(
            new Parameter("dictionary", this.currentState.selectedDictionary),
            new Parameter("call_year", "2004-02-12T15:19:21+01:00")
        ),
        400
    );

    testParameters200(
        combination,
        "Request with valid but strange value of call_year - less than 100, expect 200.",
        Arrays.asList(
            new Parameter("dictionary", this.currentState.selectedDictionary),
            new Parameter("call_year", "50")
        ),
        new CorrectResponseVerifier()
    );

    testParameters200(
        combination,
        "Request with valid but strange value of call_year - more than 1e6, expect 200.",
        Arrays.asList(
            new Parameter("dictionary", this.currentState.selectedDictionary),
            new Parameter("call_year", "1140080")
        ),
        new CorrectResponseVerifier()
    );
  }
}
