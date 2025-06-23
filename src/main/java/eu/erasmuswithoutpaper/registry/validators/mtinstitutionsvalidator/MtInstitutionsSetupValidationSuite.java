package eu.erasmuswithoutpaper.registry.validators.mtinstitutionsvalidator;

import java.util.Arrays;
import java.util.List;

import eu.erasmuswithoutpaper.registry.validators.AbstractSetupValidationSuite;
import eu.erasmuswithoutpaper.registry.validators.ApiEndpoint;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.HttpSecurityDescription;
import eu.erasmuswithoutpaper.registry.validators.ValidatedApiInfo;
import eu.erasmuswithoutpaper.registry.validators.ValidationParameter;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

class MtInstitutionsSetupValidationSuite
    extends AbstractSetupValidationSuite<MtInstitutionsSuiteState> {

  private final ValidatedApiInfo apiInfo;

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

  MtInstitutionsSetupValidationSuite(ApiValidator<MtInstitutionsSuiteState> validator,
      MtInstitutionsSuiteState state, ValidationSuiteConfig config, int version) {
    super(validator, state, config, false);

    this.apiInfo = new MtInstitutionsValidatedApiInfo(version, ApiEndpoint.NO_ENDPOINT);
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
    this.currentState.maxIds = getMaxPicIds();
    this.currentState.selectedPic = getParameterValue(PIC_PARAMETER, this::getPicParameter);
    this.currentState.selectedEcheAtDate = getParameterValue(ECHE_DATE_PARAMETER,
        this::getEcheAtDateParameter);
  }

  private String getEcheAtDateParameter() {
    return "2019-01-01";
  }

  private String getPicParameter() {
    // This is PIC of demo.usos.edu.pl, we use it as a default.
    return "999572294";
  }
}
