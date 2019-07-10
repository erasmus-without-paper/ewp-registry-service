package eu.erasmuswithoutpaper.registry.validators.mtprojectsvalidator;

import java.util.Arrays;
import java.util.List;

import eu.erasmuswithoutpaper.registry.validators.AbstractSetupValidationSuite;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.HttpSecurityDescription;
import eu.erasmuswithoutpaper.registry.validators.ValidatedApiInfo;
import eu.erasmuswithoutpaper.registry.validators.ValidationParameter;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class MtProjectsSetupValidationSuiteV1
    extends AbstractSetupValidationSuite<MtProjectsSuiteState> {

  private static final Logger logger =
      LoggerFactory.getLogger(MtProjectsSetupValidationSuiteV1.class);

  private static final ValidatedApiInfo apiInfo = new MtProjectsValidatedApiInfoV1();

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

  MtProjectsSetupValidationSuiteV1(ApiValidator<MtProjectsSuiteState> validator,
      MtProjectsSuiteState state, ValidationSuiteConfig config) {
    super(validator, state, config);
  }

  //FindBugs is not smart enough to infer that actual type of this.currentState
  //is CoursesSuiteState not just SuiteState
  @Override
  @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
  protected void runApiSpecificTests(HttpSecurityDescription securityDescription)
      throws SuiteBroken {
    if (this.currentState.parameters.contains(PIC_PARAMETER)) {
      this.currentState.selectedPic = this.currentState.parameters.get(PIC_PARAMETER);
    } else {
      this.currentState.selectedPic = getPicParameter();
    }

    if (this.currentState.parameters.contains(CALL_YEAR_PARAMETER)) {
      this.currentState.selectedCallYear = this.currentState.parameters.get(CALL_YEAR_PARAMETER);
    } else {
      this.currentState.selectedCallYear = getCallYear();
    }
  }

  private String getCallYear() {
    return Integer.toString(2019);
  }

  private String getPicParameter() {
    // This is PIC of demo.usos.edu.pl, we use it as a default.
    return "999572294";
  }
}
