package eu.erasmuswithoutpaper.registry.validators.mtprojectsvalidator;

import java.util.Arrays;
import java.util.List;

import eu.erasmuswithoutpaper.registry.validators.AbstractSetupValidationSuite;
import eu.erasmuswithoutpaper.registry.validators.ApiEndpoint;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.HttpSecurityDescription;
import eu.erasmuswithoutpaper.registry.validators.ValidatedApiInfo;
import eu.erasmuswithoutpaper.registry.validators.ValidationParameter;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class MtProjectsSetupValidationSuite
    extends AbstractSetupValidationSuite<MtProjectsSuiteState> {

  private static final Logger logger =
      LoggerFactory.getLogger(MtProjectsSetupValidationSuite.class);

  private final ValidatedApiInfo apiInfo;

  @Override
  protected Logger getLogger() {
    return logger;
  }

  @Override
  public ValidatedApiInfo getApiInfo() {
    return apiInfo;
  }

  private static final String PIC_PARAMETER = "pic";
  private static final String CALL_YEAR_PARAMETER = "call_year";

  public static List<ValidationParameter> getParameters() {
    return Arrays.asList(
        new ValidationParameter(PIC_PARAMETER),
        new ValidationParameter(CALL_YEAR_PARAMETER)
    );
  }

  MtProjectsSetupValidationSuite(ApiValidator<MtProjectsSuiteState> validator,
      MtProjectsSuiteState state, ValidationSuiteConfig config, int version) {
    super(validator, state, config);

    this.apiInfo = new MtProjectsValidatedApiInfo(version, ApiEndpoint.NO_ENDPOINT);
  }

  //FindBugs is not smart enough to infer that actual type of this.currentState
  //is CoursesSuiteState not just SuiteState
  @Override
  @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
  protected void runApiSpecificTests(HttpSecurityDescription securityDescription)
      throws SuiteBroken {
    this.currentState.selectedPic = getParameterValue(PIC_PARAMETER, this::getPicParameter);
    this.currentState.selectedCallYear = getParameterValue(CALL_YEAR_PARAMETER, this::getCallYear);
  }

  private String getCallYear() {
    return Integer.toString(2016);
  }

  private String getPicParameter() {
    // This is PIC of demo.usos.edu.pl, we use it as a default.
    return "999572294";
  }
}
