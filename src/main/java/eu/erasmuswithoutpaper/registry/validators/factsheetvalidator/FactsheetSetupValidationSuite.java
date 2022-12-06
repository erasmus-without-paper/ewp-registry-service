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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class FactsheetSetupValidationSuite
    extends AbstractSetupValidationSuite<FactsheetSuiteState> {

  private static final Logger logger =
      LoggerFactory.getLogger(FactsheetSetupValidationSuite.class);

  private final ValidatedApiInfo apiInfo;

  @Override
  protected Logger getLogger() {
    return logger;
  }

  @Override
  public ValidatedApiInfo getApiInfo() {
    return apiInfo;
  }

  private static final String HEI_ID_PARAMETER = "hei_id";

  public static List<ValidationParameter> getParameters() {
    return Arrays.asList(
        new ValidationParameter(HEI_ID_PARAMETER)
    );
  }

  FactsheetSetupValidationSuite(ApiValidator<FactsheetSuiteState> validator,
      FactsheetSuiteState state, ValidationSuiteConfig config, int version) {
    super(validator, state, config);

    this.apiInfo = new FactsheetValidatedApiInfo(version, ApiEndpoint.NoEndpoint);
  }

  private int getMaxHeiIds() {
    return getMaxIds("hei-ids");
  }

  @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
  private String getSelectedHeiId() throws SuiteBroken {
    return getCoveredHeiIds(this.currentState.url).get(0);
  }

  //FindBugs is not smart enough to infer that actual type of this.currentState
  //is CoursesSuiteState not just SuiteState
  @Override
  @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
  protected void runApiSpecificTests(HttpSecurityDescription securityDescription)
      throws SuiteBroken {
    this.currentState.maxHeiIds = getMaxHeiIds();
    this.currentState.selectedHeiId = getParameterValue(HEI_ID_PARAMETER, this::getSelectedHeiId);
  }
}
