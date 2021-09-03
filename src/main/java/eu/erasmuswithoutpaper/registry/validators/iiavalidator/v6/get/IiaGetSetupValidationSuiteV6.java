package eu.erasmuswithoutpaper.registry.validators.iiavalidator.v6.get;

import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.ValidatedApiInfo;
import eu.erasmuswithoutpaper.registry.validators.iiavalidator.IiaSuiteState;
import eu.erasmuswithoutpaper.registry.validators.iiavalidator.v2.get.IiaGetSetupValidationSuiteV2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IiaGetSetupValidationSuiteV6 extends IiaGetSetupValidationSuiteV2 {

  private static final Logger logger = LoggerFactory.getLogger(
      IiaGetSetupValidationSuiteV6.class);
  private static final ValidatedApiInfo apiInfo = new IiaGetValidatedApiInfoV6();

  /**
   * Creates a validation suite for IIAs v6 Get endpoint.
   */
  public IiaGetSetupValidationSuiteV6(ApiValidator<IiaSuiteState> validator,
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
