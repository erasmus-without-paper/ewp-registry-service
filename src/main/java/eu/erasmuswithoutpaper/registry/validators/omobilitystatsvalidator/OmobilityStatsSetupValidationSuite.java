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

  @Override
  protected ValidatedApiInfo createApiInfo(int version) {
    return new OmobilityStatsValidatedApiInfo(version, ApiEndpoint.NO_ENDPOINT);
  }


  public static List<ValidationParameter> getParameters() {
    return Collections.emptyList();
  }

  OmobilityStatsSetupValidationSuite(ApiValidator<SuiteState> validator,
      SuiteState state, ValidationSuiteConfig config, int version) {
    super(validator, state, config, true, version);
  }

  @Override
  protected boolean shouldAnonymousClientBeAllowedToAccessThisApi() {
    return false;
  }
}
