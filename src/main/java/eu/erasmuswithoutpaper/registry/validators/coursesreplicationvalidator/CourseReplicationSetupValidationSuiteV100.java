package eu.erasmuswithoutpaper.registry.validators.coursesreplicationvalidator;

import java.util.List;
import java.util.Optional;

import eu.erasmuswithoutpaper.registry.documentbuilder.KnownElement;
import eu.erasmuswithoutpaper.registry.documentbuilder.KnownNamespace;
import eu.erasmuswithoutpaper.registry.internet.Response;
import eu.erasmuswithoutpaper.registry.validators.AbstractSetupValidationSuite;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.HttpSecurityDescription;
import eu.erasmuswithoutpaper.registry.validators.InlineValidationStep;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.joox.Match;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


class CourseReplicationSetupValidationSuiteV100
    extends AbstractSetupValidationSuite<CourseReplicationSuiteState> {

  private static final Logger logger =
      LoggerFactory.getLogger(CourseReplicationSetupValidationSuiteV100.class);

  CourseReplicationSetupValidationSuiteV100(ApiValidator<CourseReplicationSuiteState> validator,
      CourseReplicationSuiteState state, ValidationSuiteConfig config) {
    super(validator, state, config);
  }

  //PMD linter forces methods returning boolean to start with 'is', it's not want we want.
  private boolean getSupportsModifiedSince() { //NOPMD
    Match match = getManifestParameter("supports-modified-since");
    if (match.isEmpty()) {
      return false;
    }
    return Boolean.parseBoolean(match.get(0).getTextContent());
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
        String url = CourseReplicationSetupValidationSuiteV100.this.currentState.url;
        List<String> coveredHeiIds =
            CourseReplicationSetupValidationSuiteV100.this.fetchHeiIdsCoveredByApiByUrl(url);

        if (coveredHeiIds.isEmpty()) {
          throw new Failure(
              "Catalogue doesn't contain any hei-ids covered by this url. We cannot preform tests.",
              Status.FAILURE, null
          );
        }

        CourseReplicationSetupValidationSuiteV100.this.currentState.selectedHeiId =
            coveredHeiIds.get(0);

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
    this.currentState.supportsModifiedSince = getSupportsModifiedSince();
    getCoveredHeiIds();
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
