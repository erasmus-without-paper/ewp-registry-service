package eu.erasmuswithoutpaper.registry.validators.coursesvalidator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import eu.erasmuswithoutpaper.registry.documentbuilder.KnownElement;
import eu.erasmuswithoutpaper.registry.documentbuilder.KnownNamespace;
import eu.erasmuswithoutpaper.registry.internet.Request;
import eu.erasmuswithoutpaper.registry.internet.Response;
import eu.erasmuswithoutpaper.registry.validators.AbstractSetupValidationSuite;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.Combination;
import eu.erasmuswithoutpaper.registry.validators.HttpSecurityDescription;
import eu.erasmuswithoutpaper.registry.validators.InlineValidationStep;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;


class CoursesSetupValidationSuiteV070
    extends AbstractSetupValidationSuite<CoursesSuiteState> {

  private static final Logger logger =
      LoggerFactory.getLogger(CoursesSetupValidationSuiteV070.class);
  private final List<String> heis = new ArrayList<>();
  private final List<String> coveredHeiIds = new ArrayList<>();
  private final List<String> coursesReplicationUrls = new ArrayList<>();


  CoursesSetupValidationSuiteV070(ApiValidator<CoursesSuiteState> validator,
      CoursesSuiteState state, ValidationSuiteConfig config) {
    super(validator, state, config);
  }

  private int getMaxLosIds() {
    return getMaxIds("los-ids");
  }

  private int getMaxLosCodes() {
    return getMaxIds("los-codes");
  }

  private void getCoveredHeiIds() throws SuiteBroken {
    this.setup(new InlineValidationStep() {
      @Override
      public String getName() {
        return "Get hei-ids covered by host managing this url.";
      }

      @Override
      @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
      protected Optional<Response> innerRun() throws Failure {
        String url = CoursesSetupValidationSuiteV070.this.currentState.url;
        coveredHeiIds.addAll(
            CoursesSetupValidationSuiteV070.this.fetchHeiIdsCoveredByApiByUrl(url)
        );

        if (coveredHeiIds.isEmpty()) {
          throw new Failure(
              "Catalogue doesn't contain any hei-ids covered by this url. We cannot preform tests.",
              Status.FAILURE, null
          );
        }
        return Optional.empty();
      }
    });
  }

  private void getCourseReplicationUrls() throws SuiteBroken {
    this.setup(new InlineValidationStep() {
      @Override
      public String getName() {
        return "Find Courses Replication API for any of covered HEIs.";
      }

      @Override
      @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
      protected Optional<Response> innerRun() throws Failure {
        for (String hei : coveredHeiIds) {
          List<String> urls =
              CoursesSetupValidationSuiteV070.this
                  .getApiUrlForHei("simple-course-replication", hei);
          if (urls != null && !urls.isEmpty()) {
            coursesReplicationUrls.add(urls.get(0));
            heis.add(hei);
          }
        }

        if (heis.isEmpty()) {
          throw new Failure("Couldn't find Courses Replication API.", Status.FAILURE, null);
        }

        return Optional.empty();
      }
    });
  }

  private void findInstitutionThatHasLosId(HttpSecurityDescription securityDescription)
      throws SuiteBroken {
    this.setup(new InlineValidationStep() {
      @Override
      public String getName() {
        return "Use Course Replication API to obtain list of Courses for one of covered HEI IDs.";
      }

      @Override
      @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
      protected Optional<Response> innerRun() throws Failure {
        // Iterate through every (course-replication url, hei id) pair we obtained earlier.
        // Find first pair such that call to `course-replication url` with `hei id` as a parameter
        // returns non-empty list of Los Ids.
        // Tests will be performed on that list of Los IDs.
        CoursesSetupValidationSuiteV070.this.currentState.selectedHeiId = null;

        for (int i = 0; i < coursesReplicationUrls.size(); i++) {
          final String url = coursesReplicationUrls.get(i);
          final String heiId = heis.get(i);
          if (url == null) {
            continue;
          }

          Element apiEntry = CoursesSetupValidationSuiteV070.this.getApiEntryFromUrl(url);
          if (apiEntry == null) {
            continue;
          }

          Request request = createRequestWithParameters(
              this,
              new Combination(
                  "GET", url, apiEntry,
                  getSecurityDescriptionFromApiEntry(apiEntry, securityDescription)
              ),
              Arrays.asList(new Parameter("hei_id", heiId))
          );
          Response response = null;
          try {
            response = CoursesSetupValidationSuiteV070.this.internet.makeRequest(request);
            expect200(response);
          } catch (IOException | Failure e) {
            continue;
          }
          List<String> coveredOunits = selectFromDocument(
              makeXmlFromBytes(response.getBody()),
              "/course-replication-response/los-id"
          );
          if (!coveredOunits.isEmpty()) {
            //Found not empty
            CoursesSetupValidationSuiteV070.this.currentState.losIds.addAll(coveredOunits);
            CoursesSetupValidationSuiteV070.this.currentState.selectedHeiId = heiId;
            break;
          }
        }

        if (CoursesSetupValidationSuiteV070.this.currentState.losIds.isEmpty()) {
          throw new Failure(
              "Cannot fetch any losIds.",
              Status.FAILURE, null
          );
        }
        return Optional.empty();
      }
    });
  }

  //FindBugs is not smart enough to infer that actual type of this.currentState
  //is CoursesSuiteState not just SuiteState
  @Override
  @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
  protected void runApiSpecificTests(HttpSecurityDescription securityDescription)
      throws SuiteBroken {
    this.currentState.maxLosIds = getMaxLosIds();
    this.currentState.maxLosCodes = getMaxLosCodes();
    this.currentState.losIds = new ArrayList<>();

    getCoveredHeiIds();
    getCourseReplicationUrls();
    findInstitutionThatHasLosId(securityDescription);
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
}
