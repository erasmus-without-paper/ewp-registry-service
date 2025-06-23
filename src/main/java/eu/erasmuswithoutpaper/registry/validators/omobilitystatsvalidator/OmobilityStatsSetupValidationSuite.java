package eu.erasmuswithoutpaper.registry.validators.omobilitystatsvalidator;

import java.util.Collections;
import java.util.List;

import eu.erasmuswithoutpaper.registry.validators.AbstractSetupValidationSuite;
import eu.erasmuswithoutpaper.registry.validators.ApiEndpoint;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.SuiteState;
import eu.erasmuswithoutpaper.registry.validators.ValidatedApiInfo;
import eu.erasmuswithoutpaper.registry.validators.ValidationParameter;

class OmobilityStatsSetupValidationSuite
    extends AbstractSetupValidationSuite<SuiteState> {

  private final ValidatedApiInfo apiInfo;

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
