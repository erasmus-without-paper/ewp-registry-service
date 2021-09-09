package eu.erasmuswithoutpaper.registry.validators.iiavalidator.v4.index;

import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.ValidatedApiInfo;
import eu.erasmuswithoutpaper.registry.validators.iiavalidator.IiaSuiteState;
import eu.erasmuswithoutpaper.registry.validators.iiavalidator.v2.index.IiaIndexBasicSetupValidationSuiteV2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IiaIndexBasicSetupValidationSuiteV4 extends IiaIndexBasicSetupValidationSuiteV2 {

  private static final Logger logger =
      LoggerFactory.getLogger(IiaIndexBasicSetupValidationSuiteV4.class);

  private static final ValidatedApiInfo apiInfo = new IiaIndexValidatedApiInfoV4();

  @Override
  protected Logger getLogger() {
    return logger;
  }

  @Override
  public ValidatedApiInfo getApiInfo() {
    return apiInfo;
  }

  /**
   * Creates a validation suite for IIAs v4 Index endpoint.
   */
  public IiaIndexBasicSetupValidationSuiteV4(ApiValidator<IiaSuiteState> validator,
                                             IiaSuiteState state, ValidationSuiteConfig config) {
    super(validator, state, config);
  }
}
