package eu.erasmuswithoutpaper.registry.validators.imobilitytorsvalidator.get;

import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.ValidatedApiInfo;
import eu.erasmuswithoutpaper.registry.validators.imobilitytorsvalidator.IMobilityTorsSuiteState;

class IMobilityTorsGetSetupValidationSuiteV2
    extends IMobilityTorsGetSetupValidationSuiteV1 {

  private static final ValidatedApiInfo apiInfo = new IMobilityTorsGetValidatedApiInfo(2);

  IMobilityTorsGetSetupValidationSuiteV2(ApiValidator<IMobilityTorsSuiteState> validator,
      IMobilityTorsSuiteState state, ValidationSuiteConfig config) {
    super(validator, state, config);
  }

  @Override
  public ValidatedApiInfo getApiInfo() {
    return apiInfo;
  }
}