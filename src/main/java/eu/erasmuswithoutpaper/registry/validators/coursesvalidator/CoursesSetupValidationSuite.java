package eu.erasmuswithoutpaper.registry.validators.coursesvalidator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import eu.erasmuswithoutpaper.registry.validators.AbstractSetupValidationSuite;
import eu.erasmuswithoutpaper.registry.validators.ApiEndpoint;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.HttpSecurityDescription;
import eu.erasmuswithoutpaper.registry.validators.ValidatedApiInfo;
import eu.erasmuswithoutpaper.registry.validators.ValidationParameter;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

class CoursesSetupValidationSuite
    extends AbstractSetupValidationSuite<CoursesSuiteState> {

  private final ValidatedApiInfo apiInfo;

  @Override
  public ValidatedApiInfo getApiInfo() {
    return apiInfo;
  }

  private static final String HEI_ID_PARAMETER = "hei_id";
  private static final String LOS_ID_PARAMETER = "los_id";

  public static List<ValidationParameter> getParameters() {
    return Arrays.asList(
        new ValidationParameter(HEI_ID_PARAMETER),
        new ValidationParameter(LOS_ID_PARAMETER, Arrays.asList(HEI_ID_PARAMETER))
    );
  }

  CoursesSetupValidationSuite(ApiValidator<CoursesSuiteState> validator,
      CoursesSuiteState state, ValidationSuiteConfig config, int version) {
    super(validator, state, config, false);

    // Version 0 uses namespaces of version 1
    this.apiInfo = new CoursesValidatedApiInfo(Math.max(1, version), ApiEndpoint.NO_ENDPOINT);
  }

  private int getMaxLosIds() {
    return getMaxIds("los-ids");
  }

  private int getMaxLosCodes() {
    return getMaxIds("los-codes");
  }

  private List<HeiIdAndUrl> getCourseReplicationUrls(List<String> heiIds)
      throws SuiteBroken {
    return getApiUrlsForHeis(
        heiIds,
        "simple-course-replication",
        ApiEndpoint.NO_ENDPOINT,
        "Find Courses Replication API for any of covered HEIs.",
        "To perform tests we need any los-id. We have to use Courses Replication API for that, "
            + "but the Catalogue doesn't contain entries for this API for any of hei-ids that we "
            + "checked."
    );
  }

  private HeiIdAndString findInstitutionThatHasLosId(
      List<HeiIdAndUrl> heiIdAndUrls,
      HttpSecurityDescription securityDescription)
      throws SuiteBroken {
    return findResponseWithString(
        heiIdAndUrls,
        securityDescription,
        "/course-replication-response/los-id",
        "Use Course Replication API to obtain list of Courses for one of covered HEI IDs.",
        "We tried to find los-id to perform tests on, but Course Replication API doesn't report "
            + "any Course for any of HEIs that we checked."
    );
  }

  //FindBugs is not smart enough to infer that actual type of this.currentState
  //is CoursesSuiteState not just SuiteState
  @Override
  @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
  protected void runApiSpecificTests(HttpSecurityDescription securityDescription)
      throws SuiteBroken {
    this.currentState.maxLosIds = getMaxLosIds();
    this.currentState.maxLosCodes = getMaxLosCodes();

    List<String> coveredHeiIds = new ArrayList<>();
    if (this.currentState.parameters.contains(HEI_ID_PARAMETER)) {
      coveredHeiIds.add(this.currentState.parameters.get(HEI_ID_PARAMETER));
    } else {
      coveredHeiIds = getCoveredHeiIds(this.currentState.url);
    }

    HeiIdAndString heiIdAndLosId = new HeiIdAndString();
    if (this.currentState.parameters.contains(LOS_ID_PARAMETER)) {
      heiIdAndLosId.heiId = this.currentState.parameters.get(HEI_ID_PARAMETER);
      heiIdAndLosId.string = this.currentState.parameters.get(LOS_ID_PARAMETER);
    } else {
      List<HeiIdAndUrl> heiIdAndReplicationUrls = getCourseReplicationUrls(coveredHeiIds);
      heiIdAndLosId = findInstitutionThatHasLosId(heiIdAndReplicationUrls, securityDescription);
    }

    this.currentState.selectedLosId = heiIdAndLosId.string;
    this.currentState.selectedHeiId = heiIdAndLosId.heiId;
  }
}
