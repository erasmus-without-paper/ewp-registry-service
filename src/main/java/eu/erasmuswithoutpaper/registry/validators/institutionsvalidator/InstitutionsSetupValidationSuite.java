package eu.erasmuswithoutpaper.registry.validators.institutionsvalidator;

import java.util.Arrays;
import java.util.List;

import eu.erasmuswithoutpaper.registry.validators.AbstractSetupValidationSuite;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.HttpSecurityDescription;
import eu.erasmuswithoutpaper.registry.validators.ValidatedApiInfo;
import eu.erasmuswithoutpaper.registry.validators.ValidationParameter;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Describes the set of test/steps to be run on an Institutions API implementation in order to
 * properly validate it.
 */
class InstitutionsSetupValidationSuite
    extends AbstractSetupValidationSuite<InstitutionsSuiteState> {

  @Override
  protected ValidatedApiInfo createApiInfo(int version) {
    return new InstitutionsValidatedApiInfo(version);
  }

  private static final String HEI_ID_PARAMETER = "hei_id";

  InstitutionsSetupValidationSuite(ApiValidator<InstitutionsSuiteState> validator,
      InstitutionsSuiteState state, ValidationSuiteConfig config, int version) {
    super(validator, state, config, false, version);
  }

  public static List<ValidationParameter> getParameters() {
    return Arrays.asList(new ValidationParameter(HEI_ID_PARAMETER));
  }

  private int getMaxHeiIds() {
    return getMaxIds("hei-ids");
  }

  //FindBugs is not smart enough to infer that actual type of this.currentState
  //is InstitutionsSuiteState not just SuiteState
  @Override
  @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
  protected void runApiSpecificTests(HttpSecurityDescription securityDescription)
      throws SuiteBroken {
    this.currentState.maxHeiIds = getMaxHeiIds();
    this.currentState.selectedHeiId = getParameterValue(HEI_ID_PARAMETER, this::getSelectedHeiId);
  }
}
