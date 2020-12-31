package eu.erasmuswithoutpaper.registry.validators.iiavalidator.v4.get;

import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.ValidatedApiInfo;
import eu.erasmuswithoutpaper.registry.validators.iiavalidator.IiaSuiteState;
import eu.erasmuswithoutpaper.registry.validators.iiavalidator.v2.get.IiaGetSetupValidationSuiteV2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IiaGetSetupValidationSuiteV4 extends IiaGetSetupValidationSuiteV2 {

  private static final Logger logger = LoggerFactory.getLogger(
      IiaGetSetupValidationSuiteV4.class);
  private static final ValidatedApiInfo apiInfo = new IiaGetValidatedApiInfoV4();

  /**
   * Creates a validation suite for IIAs v4 Get endpoint.
   */
  public IiaGetSetupValidationSuiteV4(ApiValidator<IiaSuiteState> validator,
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
