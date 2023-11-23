package eu.erasmuswithoutpaper.registry.validators.coursesreplicationvalidator;

import java.util.Optional;

import eu.erasmuswithoutpaper.registry.internet.Response;
import eu.erasmuswithoutpaper.registry.validators.AbstractValidationSuite;
import eu.erasmuswithoutpaper.registry.validators.ApiEndpoint;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.Combination;
import eu.erasmuswithoutpaper.registry.validators.InlineValidationStep;
import eu.erasmuswithoutpaper.registry.validators.ValidatedApiInfo;
import eu.erasmuswithoutpaper.registry.validators.verifiers.CorrectResponseVerifier;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Describes the set of test/steps to be run on an Institutions API implementation in order to
 * properly validate it.
 */
class CourseReplicationValidationSuite
    extends AbstractValidationSuite<CourseReplicationSuiteState> {

  private static final Logger logger =
      LoggerFactory.getLogger(CourseReplicationValidationSuite.class);

  private final ValidatedApiInfo apiInfo;

  @Override
  protected Logger getLogger() {
    return logger;
  }

  @Override
  public ValidatedApiInfo getApiInfo() {
    return apiInfo;
  }

  CourseReplicationValidationSuite(ApiValidator<CourseReplicationSuiteState> validator,
      CourseReplicationSuiteState state, ValidationSuiteConfig config, int version) {
    super(validator, state, config);

    this.apiInfo = new CourseReplicationValidatedApiInfo(version, ApiEndpoint.NO_ENDPOINT);
  }

  @Override
  //FindBugs is not smart enough to infer that actual type of this.currentState
  //is CourseReplicationSuiteState not just SuiteState
  @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
  protected void validateCombinationAny(Combination combination)
      throws SuiteBroken {
    testParameters200(
        combination,
        "Request with known hei_id, expect 200.",
        new ParameterList(
            new Parameter("hei_id", this.currentState.selectedHeiId)
        ),
        new CorrectResponseVerifier()
    );

    testParameters200(
        combination,
        "Request with known hei_id and invalid parameter, expect 200.",
        new ParameterList(
            new Parameter("hei_id", this.currentState.selectedHeiId),
            new Parameter("param_hei_id", this.currentState.selectedHeiId)
        ),
        new CorrectResponseVerifier()
    );


    testParametersError(
        combination,
        "Request with correct hei_id twice, expect 400.",
        new ParameterList(
            new Parameter("hei_id", this.currentState.selectedHeiId),
            new Parameter("hei_id", this.currentState.selectedHeiId)
        ),
        400
    );

    testParametersError(
        combination,
        "Request with single incorrect parameter, expect 400.",
        new ParameterList(new Parameter("hei_id_param", FAKE_ID)),
        400
    );

    testParametersError(
        combination,
        "Request with unknown hei_id parameter, expect 400.",
        new ParameterList(new Parameter("hei_id", FAKE_ID)),
        400
    );

    testParametersError(
        combination,
        "Request without any parameter, expect 400.",
        new ParameterList(),
        400
    );

    this.addAndRun(false, new InlineValidationStep() {
      @Override
      public String getName() {
        return "Check if this host supports modified_since.";
      }

      @Override
      @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
      protected Optional<Response> innerRun() throws Failure {
        if (currentState.supportsModifiedSince) {
          return Optional.empty();
        } else {
          throw new Failure("modified_since is not supported, some tests won't be performed.",
              Status.NOTICE, null
          );
        }
      }
    });

    if (this.currentState.supportsModifiedSince) {
      testParameters200(
          combination,
          "Request with known hei_id and correct date, expect 200.",
          new ParameterList(
              new Parameter("hei_id", this.currentState.selectedHeiId),
              new Parameter("modified_since", "2004-02-12T15:19:21+01:00")
          ),
          new CorrectResponseVerifier()
      );

      testParametersError(
          combination,
          "Request with invalid value of modified_since, expect 400.",
          new ParameterList(
              new Parameter("hei_id", this.currentState.selectedHeiId),
              new Parameter("modified_since", FAKE_ID)
          ),
          400
      );

      testParametersError(
          combination,
          "Request with modified_since being only a date, expect 400.",
          new ParameterList(
              new Parameter("hei_id", this.currentState.selectedHeiId),
              new Parameter("modified_since", "2004-02-12")
          ),
          400
      );

      testParametersError(
          combination,
          "Request with modified_since being a dateTime in wrong format, expect 400.",
          new ParameterList(
              new Parameter("hei_id", this.currentState.selectedHeiId),
              new Parameter("modified_since", "05/29/2015 05:50")
          ),
          400
      );
    }
  }
}
