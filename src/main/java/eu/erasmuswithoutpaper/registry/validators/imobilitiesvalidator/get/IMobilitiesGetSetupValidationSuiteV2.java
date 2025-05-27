package eu.erasmuswithoutpaper.registry.validators.imobilitiesvalidator.get;

import java.util.List;

import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.ValidationParameter;
import eu.erasmuswithoutpaper.registry.validators.imobilitiesvalidator.IMobilitiesSuiteState;


class IMobilitiesGetSetupValidationSuiteV2 extends IMobilitiesGetSetupValidationSuiteV1 {
  IMobilitiesGetSetupValidationSuiteV2(ApiValidator<IMobilitiesSuiteState> validator,
      IMobilitiesSuiteState state, ValidationSuiteConfig config, int version) {
    super(validator, state, config, version);
  }

  public static List<ValidationParameter> getParameters() {
    return List.of(new ValidationParameter(OMOBILITY_ID_PARAMETER));
  }
}
