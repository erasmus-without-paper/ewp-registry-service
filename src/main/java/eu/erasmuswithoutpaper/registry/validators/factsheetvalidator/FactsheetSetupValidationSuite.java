package eu.erasmuswithoutpaper.registry.validators.factsheetvalidator;

import java.util.Arrays;
import java.util.List;

import eu.erasmuswithoutpaper.registry.validators.AbstractSetupValidationSuite;
import eu.erasmuswithoutpaper.registry.validators.ApiEndpoint;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.HttpSecurityDescription;
import eu.erasmuswithoutpaper.registry.validators.ValidatedApiInfo;
import eu.erasmuswithoutpaper.registry.validators.ValidationParameter;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

class FactsheetSetupValidationSuite
    extends AbstractSetupValidationSuite<FactsheetSuiteState> {

  @Override
  protected ValidatedApiInfo createApiInfo(int version) {
    return new FactsheetValidatedApiInfo(version, ApiEndpoint.NO_ENDPOINT);
  }

  private static final String HEI_ID_PARAMETER = "hei_id";

  public static List<ValidationParameter> getParameters() {
    return Arrays.asList(
        new ValidationParameter(HEI_ID_PARAMETER)
    );
  }

  FactsheetSetupValidationSuite(ApiValidator<FactsheetSuiteState> validator,
      FactsheetSuiteState state, ValidationSuiteConfig config, int version) {
    super(validator, state, config, false, version);
  }

  private int getMaxHeiIds() {
    return getMaxIds("hei-ids");
  }

  //FindBugs is not smart enough to infer that actual type of this.currentState
  //is CoursesSuiteState not just SuiteState
  @Override
  @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
  protected void runApiSpecificTests(HttpSecurityDescription securityDescription)
      throws SuiteBroken {
    this.currentState.setMaxHeiIds(getMaxHeiIds());
    this.currentState.setSelectedHeiId(getParameterValue(HEI_ID_PARAMETER, this::getSelectedHeiId));
  }
}
