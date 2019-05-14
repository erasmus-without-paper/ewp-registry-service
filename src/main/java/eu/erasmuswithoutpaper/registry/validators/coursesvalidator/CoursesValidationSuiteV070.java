package eu.erasmuswithoutpaper.registry.validators.coursesvalidator;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import eu.erasmuswithoutpaper.registry.documentbuilder.KnownElement;
import eu.erasmuswithoutpaper.registry.documentbuilder.KnownNamespace;
import eu.erasmuswithoutpaper.registry.internet.Request;
import eu.erasmuswithoutpaper.registry.internet.Response;
import eu.erasmuswithoutpaper.registry.validators.AbstractValidationSuite;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.Combination;
import eu.erasmuswithoutpaper.registry.validators.InlineValidationStep;
import eu.erasmuswithoutpaper.registry.validators.ValidationStepWithStatus.Status;
import eu.erasmuswithoutpaper.registry.validators.verifiers.ListEqualVerifier;
import eu.erasmuswithoutpaper.registry.validators.verifiers.NotInListVerifier;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

/**
 * Describes the set of test/steps to be run on an Institutions API implementation in order to
 * properly validate it.
 */
class CoursesValidationSuiteV070
    extends AbstractValidationSuite<CoursesSuiteState> {

  private static final Logger logger =
      LoggerFactory.getLogger(
          CoursesValidationSuiteV070.class);

  CoursesValidationSuiteV070(ApiValidator<CoursesSuiteState> validator,
      CoursesSuiteState state, ValidationSuiteConfig config) {
    super(validator, state, config);
  }

  static class RequestFields {
    public LocalDate startDate = null;
    public LocalDate endDate = null;
    public String loiId = null;
  }

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
        String losId = CoursesValidationSuiteV070.this.currentState.losIds.get(0);
        Request request = createRequestWithParameters(this, combination,
            Arrays.asList(
                new Parameter("hei_id", CoursesValidationSuiteV070.this.currentState.selectedHeiId),
                new Parameter("los_id", losId)
            )
        );

        List<String> expectedIDs =
            Collections.singletonList(CoursesValidationSuiteV070.this.currentState.losIds.get(0));
        Response response = verifyResponse(
            this, combination, request, new CoursesIdsVerifier(expectedIDs)
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

    generalTestsIdsAndCodes(combination, "los", this.currentState.selectedHeiId,
        this.currentState.losIds.get(0), losCode,
        this.currentState.maxLosIds, this.currentState.maxLosCodes, CoursesIdsVerifier::new
    );

    testParametersError(
        combination,
        "Request with correct hei_id and incorrect hei_id, expect 400.",
        Arrays.asList(
            new Parameter("hei_id", this.currentState.selectedHeiId),
            new Parameter("hei_id", fakeId),
            new Parameter("los_id", this.currentState.losIds.get(0))
        ),
        400
    );

    if (requestFields.startDate != null) {
      String losId = this.currentState.losIds.get(0);

      testParameters200(
          combination,
          "Ask for LOIs after one of the courses started, expect it is not included in results.",
          Arrays.asList(
              new Parameter("hei_id", this.currentState.selectedHeiId),
              new Parameter("los_id", this.currentState.losIds.get(0)),
              new Parameter("lois_after", requestFields.startDate.plusDays(1).toString())
          ),
          new LoiIdNotIncludedVerifier(losId, requestFields.loiId, Status.WARNING)
      );

      testParameters200(
          combination,
          "Ask for LOIs before one of the courses ended, expect it is not included in results.",
          Arrays.asList(
              new Parameter("hei_id", this.currentState.selectedHeiId),
              new Parameter("los_id", this.currentState.losIds.get(0)),
              new Parameter("lois_before", requestFields.endDate.minusDays(1).toString())
          ),
          new LoiIdNotIncludedVerifier(losId, requestFields.loiId, Status.WARNING)
      );
    }

    testParametersError(
        combination,
        "Multiple lois_before parameters, expect 400.",
        Arrays.asList(
            new Parameter("hei_id", this.currentState.selectedHeiId),
            new Parameter("los_id", this.currentState.losIds.get(0)),
            new Parameter("lois_before", "2010-01-01"),
            new Parameter("lois_before", "2010-01-01")
        ),
        400
    );

    testParametersError(
        combination,
        "Multiple lois_after parameters, expect 400.",
        Arrays.asList(
            new Parameter("hei_id", this.currentState.selectedHeiId),
            new Parameter("los_id", this.currentState.losIds.get(0)),
            new Parameter("lois_after", "2010-01-01"),
            new Parameter("lois_after", "2010-01-01")
        ),
        400
    );

    testParametersError(
        combination,
        "lois_before parameter is not a date, expect 400.",
        Arrays.asList(
            new Parameter("hei_id", this.currentState.selectedHeiId),
            new Parameter("los_id", this.currentState.losIds.get(0)),
            new Parameter("lois_before", "abcd-ef-gh")
        ),
        400
    );

    testParametersError(
        combination,
        "lois_before has format dd-MM-yyyy, expect 400.",
        Arrays.asList(
            new Parameter("hei_id", this.currentState.selectedHeiId),
            new Parameter("los_id", this.currentState.losIds.get(0)),
            new Parameter("lois_before", "31-12-2019")
        ),
        400
    );

    testParametersError(
        combination,
        "lois_before consists of date and time, expect 400.",
        Arrays.asList(
            new Parameter("hei_id", this.currentState.selectedHeiId),
            new Parameter("los_id", this.currentState.losIds.get(0)),
            new Parameter("lois_before", "2009-12-31 23:59:59")
        ),
        400
    );
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
    return KnownElement.RESPONSE_COURSES_V1;
  }

  @Override
  protected String getApiNamespace() {
    return KnownNamespace.APIENTRY_COURSES_V1.getNamespaceUri();
  }

  @Override
  protected String getApiName() {
    return "courses";
  }

  @Override
  public String getApiPrefix() {
    return "co1";
  }

  @Override
  public String getApiResponsePrefix() {
    return "cor1";
  }


  private static class CoursesIdsVerifier extends ListEqualVerifier {
    CoursesIdsVerifier(List<String> expected, Status status) {
      super(expected, status);
    }

    CoursesIdsVerifier(List<String> expected) {
      super(expected, Status.FAILURE);
    }

    @Override
    protected List<String> getSelector() {
      return Arrays.asList("learningOpportunitySpecification", "los-id");
    }

    @Override
    protected String getParamName() {
      return "los-id";
    }
  }


  private static class LoiIdNotIncludedVerifier extends NotInListVerifier {
    private final String losId;

    LoiIdNotIncludedVerifier(String losId, String loiId, Status status) {
      super(loiId, status);
      this.losId = losId;
    }

    @Override
    protected List<String> getSelector() {
      return Arrays.asList(
          "learningOpportunitySpecification",
          "los-id[text()=\"" + losId + "\"]/..",
          "specifies",
          "learningOpportunityInstance",
          "loi-id"
      );
    }

    @Override
    protected String getParamName() {
      return "los-id";
    }
  }
}
