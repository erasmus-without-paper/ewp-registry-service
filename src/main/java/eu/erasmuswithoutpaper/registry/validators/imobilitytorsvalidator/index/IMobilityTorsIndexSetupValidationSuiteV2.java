package eu.erasmuswithoutpaper.registry.validators.imobilitytorsvalidator.index;

import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.ValidatedApiInfo;
import eu.erasmuswithoutpaper.registry.validators.imobilitytorsvalidator.IMobilityTorsSuiteState;


class IMobilityTorsIndexSetupValidationSuiteV2
    extends IMobilityTorsIndexSetupValidationSuiteV1 {

  private static final ValidatedApiInfo apiInfo = new IMobilityTorsIndexValidatedApiInfo(2);

  IMobilityTorsIndexSetupValidationSuiteV2(ApiValidator<IMobilityTorsSuiteState> validator,
      IMobilityTorsSuiteState state, ValidationSuiteConfig config) {
    super(validator, state, config);
  }

  @Override
  public ValidatedApiInfo getApiInfo() {
    return apiInfo;
  }
}