package eu.erasmuswithoutpaper.registry.validators.mtprojectsvalidator;

import eu.erasmuswithoutpaper.registry.validators.AbstractValidationSuite;
import eu.erasmuswithoutpaper.registry.validators.ApiEndpoint;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.Combination;
import eu.erasmuswithoutpaper.registry.validators.ValidatedApiInfo;
import eu.erasmuswithoutpaper.registry.validators.verifiers.CorrectResponseVerifier;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Describes the set of test/steps to be run on an MT Projects API implementation in order to
 * properly validate it.
 */
class MtProjectsValidationSuite extends AbstractValidationSuite<MtProjectsSuiteState> {

  @Override
  protected ValidatedApiInfo createApiInfo(int version) {
    return new MtProjectsValidatedApiInfo(version, ApiEndpoint.NO_ENDPOINT);
  }

  MtProjectsValidationSuite(ApiValidator<MtProjectsSuiteState> validator,
      MtProjectsSuiteState state, ValidationSuiteConfig config, int version) {
    super(validator, state, config, version);
  }

  @Override
  //FindBugs is not smart enough to infer that actual type of this.currentState
  //is MtProjectsSuiteState not just SuiteState
  @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
  protected void validateCombinationAny(Combination combination)
      throws SuiteBroken {
    testParameters200(
        combination,
        "Request with known pic and call_year, expect 200 and non empty response.",
        new ParameterList(
            new Parameter("pic", this.currentState.selectedPic),
            new Parameter("call_year", this.currentState.selectedCallYear)
        ),
        new CorrectResponseVerifier()
    );

    testParameters200(
        combination,
        "Request with known pic, call_year and invalid parameter, expect 200.",
        new ParameterList(
            new Parameter("pic", this.currentState.selectedPic),
            new Parameter("call_year", this.currentState.selectedCallYear),
            new Parameter("pic_param", this.currentState.selectedPic)
        ),
        new CorrectResponseVerifier()
    );

    testParametersError(
        combination,
        "Request with correct pic twice, expect 400.",
        new ParameterList(
            new Parameter("pic", this.currentState.selectedPic),
            new Parameter("pic", this.currentState.selectedPic),
            new Parameter("call_year", this.currentState.selectedCallYear)
        ),
        400
    );

    testParametersError(
        combination,
        "Request with correct call_year twice, expect 400.",
        new ParameterList(
            new Parameter("pic", this.currentState.selectedPic),
            new Parameter("call_year", this.currentState.selectedCallYear),
            new Parameter("call_year", this.currentState.selectedCallYear)
        ),
        400
    );

    testParametersError(
        combination,
        "Request with single incorrect parameter, expect 400.",
        new ParameterList(new Parameter("pic_param", FAKE_ID)),
        400
    );

    testParametersError(
        combination,
        "Request without pic, expect 400.",
        new ParameterList(
            new Parameter("call_year", this.currentState.selectedCallYear)
        ),
        400
    );

    testParametersError(
        combination,
        "Request without call_year, expect 400.",
        new ParameterList(
            new Parameter("pic", this.currentState.selectedPic)
        ),
        400
    );

    testParametersError(
        combination,
        "Request without any parameter, expect 400.",
        new ParameterList(),
        400
    );

    testParametersError(
        combination,
        "Request with unknown pic parameter, expect 400.",
        new ParameterList(
            new Parameter("pic", FAKE_ID),
            new Parameter("call_year", this.currentState.selectedCallYear)
        ),
        400
    );

    testParametersError(
        combination,
        "Request with invalid value of call_year - not a number, expect 400.",
        new ParameterList(
            new Parameter("pic", this.currentState.selectedPic),
            new Parameter("call_year", "not-a-number")
        ),
        400
    );

    testParameters200(
        combination,
        "Request with call_year equal zero, expect 200.",
        new ParameterList(
            new Parameter("pic", this.currentState.selectedPic),
            new Parameter("call_year", "0")
        ),
        new CorrectResponseVerifier()
    );

    testParameters200(
        combination,
        "Request with negative call_year, expect 200.",
        new ParameterList(
            new Parameter("pic", this.currentState.selectedPic),
            new Parameter("call_year", "-2019")
        ),
        new CorrectResponseVerifier()
    );

    testParametersError(
        combination,
        "Request with call_year being a date, expect 400.",
        new ParameterList(
            new Parameter("pic", this.currentState.selectedPic),
            new Parameter("call_year", "2004-02-12")
        ),
        400
    );

    testParametersError(
        combination,
        "Request with call_year being a date and time, expect 400.",
        new ParameterList(
            new Parameter("pic", this.currentState.selectedPic),
            new Parameter("call_year", "2004-02-12T15:19:21+01:00")
        ),
        400
    );

    testParameters200(
        combination,
        "Request with valid but strange value of call_year - less than 100, expect 200.",
        new ParameterList(
            new Parameter("pic", this.currentState.selectedPic),
            new Parameter("call_year", "50")
        ),
        new CorrectResponseVerifier()
    );

    testParameters200(
        combination,
        "Request with valid but strange value of call_year - more than 1e6, expect 200.",
        new ParameterList(
            new Parameter("pic", this.currentState.selectedPic),
            new Parameter("call_year", "1140080")
        ),
        new CorrectResponseVerifier()
    );
  }
}
