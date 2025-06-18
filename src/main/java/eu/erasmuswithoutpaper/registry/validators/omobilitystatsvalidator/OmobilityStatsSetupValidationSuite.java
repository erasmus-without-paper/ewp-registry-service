package eu.erasmuswithoutpaper.registry.validators.omobilitystatsvalidator;

import java.util.Collections;
import java.util.List;

import eu.erasmuswithoutpaper.registry.validators.AbstractSetupValidationSuite;
import eu.erasmuswithoutpaper.registry.validators.ApiEndpoint;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.SuiteState;
import eu.erasmuswithoutpaper.registry.validators.ValidatedApiInfo;
import eu.erasmuswithoutpaper.registry.validators.ValidationParameter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class OmobilityStatsSetupValidationSuite
    extends AbstractSetupValidationSuite<SuiteState> {

  private static final Logger logger =
      LoggerFactory.getLogger(OmobilityStatsSetupValidationSuite.class);

  private final ValidatedApiInfo apiInfo;

  @Override
  protected Logger getLogger() {
    return logger;
  }

  @Override
  public ValidatedApiInfo getApiInfo() {
    return apiInfo;
  }


  public static List<ValidationParameter> getParameters() {
    return Collections.emptyList();
  }

  OmobilityStatsSetupValidationSuite(ApiValidator<SuiteState> validator,
      SuiteState state, ValidationSuiteConfig config, int version) {
    super(validator, state, config, true);

    this.apiInfo = new OmobilityStatsValidatedApiInfo(version, ApiEndpoint.NO_ENDPOINT);
  }

  @Override
  protected boolean shouldAnonymousClientBeAllowedToAccessThisApi() {
    return false;
  }
}
