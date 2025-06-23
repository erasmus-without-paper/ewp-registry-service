package eu.erasmuswithoutpaper.registry.validators.coursesreplicationvalidator;

import java.util.Arrays;
import java.util.List;

import eu.erasmuswithoutpaper.registry.validators.AbstractSetupValidationSuite;
import eu.erasmuswithoutpaper.registry.validators.ApiEndpoint;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.HttpSecurityDescription;
import eu.erasmuswithoutpaper.registry.validators.ValidatedApiInfo;
import eu.erasmuswithoutpaper.registry.validators.ValidationParameter;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.joox.Match;

class CourseReplicationSetupValidationSuite
    extends AbstractSetupValidationSuite<CourseReplicationSuiteState> {

  @Override
  public ValidatedApiInfo createApiInfo(int version) {
    return new CourseReplicationValidatedApiInfo(version, ApiEndpoint.NO_ENDPOINT);
  }

  private static final String HEI_ID_PARAMETER = "hei_id";

  public static List<ValidationParameter> getParameters() {
    return Arrays.asList(
        new ValidationParameter(HEI_ID_PARAMETER)
    );
  }

  CourseReplicationSetupValidationSuite(ApiValidator<CourseReplicationSuiteState> validator,
      CourseReplicationSuiteState state, ValidationSuiteConfig config, int version) {
    super(validator, state, config, false, version);
  }


  //PMD linter forces methods returning boolean to start with 'is', it's not want we want.
  private boolean getSupportsModifiedSince() { //NOPMD
    Match match = getManifestParameter("supports-modified-since");
    if (match.isEmpty()) {
      return false;
    }
    return Boolean.parseBoolean(match.get(0).getTextContent());
  }

  //FindBugs is not smart enough to infer that actual type of this.currentState
  //is CoursesSuiteState not just SuiteState
  @Override
  @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
  protected void runApiSpecificTests(HttpSecurityDescription securityDescription)
      throws SuiteBroken {
    this.currentState.supportsModifiedSince = getSupportsModifiedSince();
    this.currentState.selectedHeiId = getParameterValue(HEI_ID_PARAMETER, this::getSelectedHeiId);
  }
}
