package eu.erasmuswithoutpaper.registry.validators.mtinstitutionsvalidator;

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

class MtInstitutionsSetupValidationSuiteV1
    extends AbstractSetupValidationSuite<MtInstitutionsSuiteState> {

  private static final Logger logger =
      LoggerFactory.getLogger(MtInstitutionsSetupValidationSuiteV1.class);

  private static final ValidatedApiInfo apiInfo = new MtInstitutionsValidatedApiInfoV1();

  @Override
  protected Logger getLogger() {
    return logger;
  }

  @Override
  public ValidatedApiInfo getApiInfo() {
    return apiInfo;
  }

  private static final String PIC_PARAMETER = "pic";
  private static final String ECHE_DATE_PARAMETER = "eche_at_date";

  public static List<ValidationParameter> getParameters() {
    return Arrays.asList(
        new ValidationParameter(PIC_PARAMETER),
        new ValidationParameter(ECHE_DATE_PARAMETER)
    );
  }

  MtInstitutionsSetupValidationSuiteV1(ApiValidator<MtInstitutionsSuiteState> validator,
      MtInstitutionsSuiteState state, ValidationSuiteConfig config) {
    super(validator, state, config);
  }

  private int getMaxPicIds() {
    return getMaxIds("ids");
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

    if (this.currentState.parameters.contains(ECHE_DATE_PARAMETER)) {
      this.currentState.selectedEcheAtDate = this.currentState.parameters.get(ECHE_DATE_PARAMETER);
    } else {
      this.currentState.selectedEcheAtDate = getEcheAtDateParameter();
    }

    this.currentState.maxIds = getMaxPicIds();
  }

  private String getEcheAtDateParameter() {
    return "2019-01-01";
  }

  private String getPicParameter() {
    // This is PIC of demo.usos.edu.pl, we use it as a default.
    return "999572294";
  }
}
