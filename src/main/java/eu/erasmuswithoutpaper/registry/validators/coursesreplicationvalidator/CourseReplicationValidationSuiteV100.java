package eu.erasmuswithoutpaper.registry.validators.coursesreplicationvalidator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;

import eu.erasmuswithoutpaper.registry.documentbuilder.KnownElement;
import eu.erasmuswithoutpaper.registry.documentbuilder.KnownNamespace;
import eu.erasmuswithoutpaper.registry.internet.Response;
import eu.erasmuswithoutpaper.registry.validators.AbstractValidationSuite;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.Combination;
import eu.erasmuswithoutpaper.registry.validators.InlineValidationStep;
import eu.erasmuswithoutpaper.registry.validators.verifiers.NoopVerifier;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Describes the set of test/steps to be run on an Institutions API implementation in order to
 * properly validate it.
 */
class CourseReplicationValidationSuiteV100
    extends AbstractValidationSuite<CourseReplicationSuiteState> {

  private static final Logger logger =
      LoggerFactory.getLogger(
          CourseReplicationValidationSuiteV100.class);

  CourseReplicationValidationSuiteV100(ApiValidator<CourseReplicationSuiteState> validator,
      CourseReplicationSuiteState state, ValidationSuiteConfig config) {
    super(validator, state, config);
  }

  //FindBugs is not smart enough to infer that actual type of this.currentState
  //is CourseReplicationSuiteState not just SuiteState
  @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
  protected void validateCombinationAny(Combination combination)
      throws SuiteBroken {
    testParameters200(
        combination,
        "Request with known hei_id, expect 200.",
        Arrays.asList(
            new Parameter("hei_id", this.currentState.selectedHeiId)
        ),
        new NoopVerifier()
    );

    testParameters200(
        combination,
        "Request with known hei_id and invalid parameter, expect 200.",
        Arrays.asList(
            new Parameter("hei_id", this.currentState.selectedHeiId),
            new Parameter("param_hei_id", this.currentState.selectedHeiId)
        ),
        new NoopVerifier()
    );


    testParametersError(
        combination,
        "Request with correct hei_id twice, expect 400.",
        Arrays.asList(
            new Parameter("hei_id", this.currentState.selectedHeiId),
            new Parameter("hei_id", this.currentState.selectedHeiId)
        ),
        400
    );

    testParametersError(
        combination,
        "Request with single incorrect parameter, expect 400.",
        Arrays.asList(new Parameter("hei_id_param", fakeId)),
        400
    );

    testParametersError(
        combination,
        "Request with unknown hei_id parameter, expect 400.",
        Arrays.asList(new Parameter("hei_id", fakeId)),
        400
    );

    testParametersError(
        combination,
        "Request without any parameter, expect 400.",
        new ArrayList<>(),
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
        if (CourseReplicationValidationSuiteV100.this.currentState.supportsModifiedSince) {
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
          Arrays.asList(
              new Parameter("hei_id", this.currentState.selectedHeiId),
              new Parameter("modified_since", "2004-02-12T15:19:21+01:00")
          ),
          new NoopVerifier()
      );

      testParametersError(
          combination,
          "Request with invalid value of modified_since, expect 400.",
          Arrays.asList(
              new Parameter("hei_id", this.currentState.selectedHeiId),
              new Parameter("modified_since", fakeId)
          ),
          400
      );

      testParametersError(
          combination,
          "Request with modified_since being only a date, expect 400.",
          Arrays.asList(
              new Parameter("hei_id", this.currentState.selectedHeiId),
              new Parameter("modified_since", "2004-02-12")
          ),
          400
      );

      testParametersError(
          combination,
          "Request with modified_since being a dateTime in wrong format, expect 400.",
          Arrays.asList(
              new Parameter("hei_id", this.currentState.selectedHeiId),
              new Parameter("modified_since", "05/29/2015 05:50")
          ),
          400
      );
    }
  }

  @Override
  protected void validateCombinationPost(Combination combination)
      throws SuiteBroken {
    this.addAndRun(
        false,
        this.createHttpMethodValidationStep(combination.withChangedHttpMethod("PUT"))
    );
    this.addAndRun(
        false,
        this.createHttpMethodValidationStep(combination.withChangedHttpMethod("DELETE"))
    );
    validateCombinationAny(combination);
  }

  @Override
  protected void validateCombinationGet(Combination combination)
      throws SuiteBroken {
    validateCombinationAny(combination);
  }

  @Override
  protected Logger getLogger() {
    return logger;
  }

  @Override
  protected KnownElement getKnownElement() {
    return KnownElement.RESPONSE_COURSE_REPLICATION_V1;
  }

  @Override
  protected String getApiNamespace() {
    return KnownNamespace.APIENTRY_COURSE_REPLICATION_V1.getNamespaceUri();
  }

  @Override
  protected String getApiName() {
    return "simple-course-replication";
  }

  @Override
  public String getApiPrefix() {
    return "cr1";
  }

  @Override
  public String getApiResponsePrefix() {
    return "crr1";
  }
}
