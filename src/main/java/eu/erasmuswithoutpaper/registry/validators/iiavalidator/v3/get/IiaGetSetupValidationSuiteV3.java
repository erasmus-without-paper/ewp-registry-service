package eu.erasmuswithoutpaper.registry.validators.iiavalidator.v3.get;

import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.ValidatedApiInfo;
import eu.erasmuswithoutpaper.registry.validators.iiavalidator.IiaSuiteState;
import eu.erasmuswithoutpaper.registry.validators.iiavalidator.v2.get.IiaGetSetupValidationSuiteV2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IiaGetSetupValidationSuiteV3 extends IiaGetSetupValidationSuiteV2 {

  private static final Logger logger = LoggerFactory.getLogger(
      IiaGetSetupValidationSuiteV3.class);
  private static final ValidatedApiInfo apiInfo = new IiaGetValidatedApiInfoV3();

  /**
   * Creates a validation suite for IIAs v3 Get endpoint.
   */
  public IiaGetSetupValidationSuiteV3(ApiValidator<IiaSuiteState> validator,
      IiaSuiteState state,
      ValidationSuiteConfig config) {
    super(validator, state, config);
  }

  @Override
  protected Logger getLogger() {
    return logger;
  }

  @Override
  public ValidatedApiInfo getApiInfo() {
    return apiInfo;
  }
}
