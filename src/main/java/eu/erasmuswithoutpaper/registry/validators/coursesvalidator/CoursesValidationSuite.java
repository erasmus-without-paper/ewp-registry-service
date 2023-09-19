package eu.erasmuswithoutpaper.registry.validators.coursesvalidator;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import eu.erasmuswithoutpaper.registry.internet.Request;
import eu.erasmuswithoutpaper.registry.internet.Response;
import eu.erasmuswithoutpaper.registry.validators.AbstractValidationSuite;
import eu.erasmuswithoutpaper.registry.validators.ApiEndpoint;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.Combination;
import eu.erasmuswithoutpaper.registry.validators.InlineValidationStep;
import eu.erasmuswithoutpaper.registry.validators.ValidatedApiInfo;
import eu.erasmuswithoutpaper.registry.validators.ValidationStepWithStatus.Status;
import eu.erasmuswithoutpaper.registry.validators.verifiers.NotInListVerifier;
import eu.erasmuswithoutpaper.registry.validators.verifiers.VerifierFactory;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

/**
 * Describes the set of test/steps to be run on an Institutions API implementation in order to
 * properly validate it.
 */
class CoursesValidationSuite
    extends AbstractValidationSuite<CoursesSuiteState> {

  private static final Logger logger =
      LoggerFactory.getLogger(CoursesSetupValidationSuite.class);

  private final ValidatedApiInfo apiInfo;

  @Override
  protected Logger getLogger() {
    return logger;
  }

  @Override
  public ValidatedApiInfo getApiInfo() {
    return apiInfo;
  }

  CoursesValidationSuite(ApiValidator<CoursesSuiteState> validator,
      CoursesSuiteState state, ValidationSuiteConfig config, int version) {
    super(validator, state, config);

    this.apiInfo = new CoursesValidatedApiInfo(Math.max(1, version), ApiEndpoint.NoEndpoint);
  }

  static class RequestFields {
    public LocalDate startDate = null;
    public LocalDate endDate = null;
    public String loiId = null;
  }

  @Override
  //FindBugs is not smart enough to infer that actual type of this.currentState
  //is CoursesSuiteState not just SuiteState
  @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
  protected void validateCombinationAny(Combination combination)
      throws SuiteBroken {
    ArrayList<String> losCodes = new ArrayList<>();
    RequestFields requestFields = new RequestFields();

    this.addAndRun(false, new InlineValidationStep() {
      @Override
      public String getName() {
        return "Request for one of known los-ids, expect 200 OK.";
      }

      @Override
      @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
      protected Optional<Response> innerRun() throws Failure {
        String losId = CoursesValidationSuite.this.currentState.selectedLosId;
        Request request = createRequestWithParameters(this, combination,
            new ParameterList(
                new Parameter("hei_id", CoursesValidationSuite.this.currentState.selectedHeiId),
                new Parameter("los_id", losId)
            )
        );

        List<String> expectedIDs =
            Collections.singletonList(CoursesValidationSuite.this.currentState.selectedLosId);
        Response response = makeRequestAndVerifyResponse(
            this, combination, request,
            losIdVerifierFactory.expectResponseToContainExactly(expectedIDs)
        );

        Element body = makeXmlFromBytes(response.getBody());

        List<String> codes = selectFromDocument(
            body,
            "/courses-response/learningOpportunitySpecification/los-id[text()=\"" + losId + "\"]"
                + "/../los-code"
        );

        if (codes.isEmpty()) {
          throw new Failure("It is RECOMMENDED to include los-code in response.\n"
              + "Some tests won't be performed without this value.", Status.WARNING, response);
        }
        losCodes.addAll(codes);

        List<String> instances = selectFromDocument(
            body,
            "/courses-response/learningOpportunitySpecification/los-id[text()=\"" + losId + "\"]"
                + "/../specifies/learningOpportunityInstance/loi-id"
        );

        if (instances.isEmpty()) {
          throw new Failure(
              "No learningOpportunityInstance returned by the server, some tests will be skipped",
              Status.NOTICE, response
          );
        }

        requestFields.loiId = instances.get(0);

        List<String> startDates = selectFromDocument(
            body,
            "/courses-response/learningOpportunitySpecification/los-id[text()=\"" + losId + "\"]"
                + "/../specifies/learningOpportunityInstance/loi-id[text()=\""
                + requestFields.loiId + "\"]"
                + "/../start"
        );
        List<String> endDates = selectFromDocument(
            body,
            "/courses-response/learningOpportunitySpecification/los-id[text()=\"" + losId + "\"]"
                + "/../specifies/learningOpportunityInstance/loi-id[text()=\""
                + requestFields.loiId + "\"]"
                + "/../end"
        );

        // Assuming there are start and end elements, as they are required.
        DateTimeFormatter pattern = DateTimeFormatter.ofPattern("yyyy-MM-dd[XXX]");
        LocalDate start = LocalDate.from(pattern.parse(startDates.get(0)));
        LocalDate end = LocalDate.from(pattern.parse(endDates.get(0)));
        requestFields.startDate = start;
        requestFields.endDate = end;

        return Optional.of(response);
      }
    });

    String losCode = null;
    if (!losCodes.isEmpty()) {
      losCode = losCodes.get(0);
    }

    generalTestsIdsAndCodes(combination,
        this.currentState.selectedHeiId,
        "los",
        this.currentState.selectedLosId, this.currentState.maxLosIds,
        losCode, this.currentState.maxLosCodes,
        losIdVerifierFactory
    );

    String losId = this.currentState.selectedLosId;

    String dayAfterStart = null;
    String dayBeforeEnd = null;
    if (requestFields.startDate != null) {
      dayAfterStart = requestFields.startDate.plusDays(1).toString();
      dayBeforeEnd = requestFields.endDate.minusDays(1).toString();
    }

    testParameters200(
        combination,
        "Ask for LOIs after one of the courses started, expect it is not included in results.",
        new ParameterList(
            new Parameter("hei_id", this.currentState.selectedHeiId),
            new Parameter("los_id", this.currentState.selectedLosId),
            new Parameter("lois_after", dayAfterStart)
        ),
        new LoiIdNotIncludedVerifier(losId, requestFields.loiId),
        Status.WARNING,
        requestFields.startDate == null,
        "No LOI ID."
    );

    testParameters200(
        combination,
        "Ask for LOIs before one of the courses ended, expect it is not included in results.",
        new ParameterList(
            new Parameter("hei_id", this.currentState.selectedHeiId),
            new Parameter("los_id", this.currentState.selectedLosId),
            new Parameter("lois_before", dayBeforeEnd)
        ),
        new LoiIdNotIncludedVerifier(losId, requestFields.loiId),
        Status.WARNING,
        requestFields.startDate == null,
        "No LOI ID."
    );

    testParametersError(
        combination,
        "Multiple lois_before parameters, expect 400.",
        new ParameterList(
            new Parameter("hei_id", this.currentState.selectedHeiId),
            new Parameter("los_id", this.currentState.selectedLosId),
            new Parameter("lois_before", "2010-01-01"),
            new Parameter("lois_before", "2010-01-01")
        ),
        400
    );

    testParametersError(
        combination,
        "Multiple lois_after parameters, expect 400.",
        new ParameterList(
            new Parameter("hei_id", this.currentState.selectedHeiId),
            new Parameter("los_id", this.currentState.selectedLosId),
            new Parameter("lois_after", "2010-01-01"),
            new Parameter("lois_after", "2010-01-01")
        ),
        400
    );

    testParametersError(
        combination,
        "lois_before parameter is not a date, expect 400.",
        new ParameterList(
            new Parameter("hei_id", this.currentState.selectedHeiId),
            new Parameter("los_id", this.currentState.selectedLosId),
            new Parameter("lois_before", "abcd-ef-gh")
        ),
        400
    );

    testParametersError(
        combination,
        "lois_before has format dd-MM-yyyy, expect 400.",
        new ParameterList(
            new Parameter("hei_id", this.currentState.selectedHeiId),
            new Parameter("los_id", this.currentState.selectedLosId),
            new Parameter("lois_before", "31-12-2019")
        ),
        400
    );

    testParametersError(
        combination,
        "lois_before consists of date and time, expect 400.",
        new ParameterList(
            new Parameter("hei_id", this.currentState.selectedHeiId),
            new Parameter("los_id", this.currentState.selectedLosId),
            new Parameter("lois_before", "2009-12-31 23:59:59")
        ),
        400
    );
  }

  private VerifierFactory losIdVerifierFactory = new VerifierFactory(
      Arrays.asList("learningOpportunitySpecification", "los-id"));

  private static class LoiIdNotIncludedVerifier extends NotInListVerifier {
    private static List<String> createSelectorForLosId(String losId) {
      return Arrays.asList(
          "learningOpportunitySpecification",
          "los-id[text()=\"" + losId + "\"]/..",
          "specifies",
          "learningOpportunityInstance",
          "loi-id"
      );
    }

    LoiIdNotIncludedVerifier(String losId, String loiId) {
      super(loiId, createSelectorForLosId(losId));
    }
  }
}
