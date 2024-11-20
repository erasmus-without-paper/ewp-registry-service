package eu.erasmuswithoutpaper.registry.validators.iiacnrvalidator;

import java.util.List;

import eu.erasmuswithoutpaper.registry.validators.AbstractSetupValidationSuite;
import eu.erasmuswithoutpaper.registry.validators.ApiEndpoint;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.SuiteState;
import eu.erasmuswithoutpaper.registry.validators.ValidatedApiInfo;
import eu.erasmuswithoutpaper.registry.validators.ValidationParameter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class IiaCnrSetupValidationSuite
    extends AbstractSetupValidationSuite<SuiteState> {

  private static final Logger logger =
      LoggerFactory.getLogger(IiaCnrSetupValidationSuite.class);

  private final ValidatedApiInfo apiInfo;

  @Override
  protected Logger getLogger() {
    return logger;
  }

  @Override
  public ValidatedApiInfo getApiInfo() {
    return apiInfo;
  }

  private static final String IIA_ID_PARAMETER = "iia_id";

  public static List<ValidationParameter> getParameters() {
    return List.of(new ValidationParameter(IIA_ID_PARAMETER));
  }

  IiaCnrSetupValidationSuite(ApiValidator<SuiteState> validator, SuiteState state,
      ValidationSuiteConfig config, int version) {
    super(validator, state, config, true);

    this.apiInfo = new IiaCnrValidatedApiInfo(version, ApiEndpoint.NO_ENDPOINT);
  }
}
