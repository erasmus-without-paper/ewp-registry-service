package eu.erasmuswithoutpaper.registry.validators.iiavalidator.v6.index;

import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.ValidatedApiInfo;
import eu.erasmuswithoutpaper.registry.validators.iiavalidator.IiaSuiteState;
import eu.erasmuswithoutpaper.registry.validators.iiavalidator.v2.index.IiaIndexBasicValidationSuiteV2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Describes the set of test/steps to be run on an IIAs API index endpoint implementation in order
 * to properly validate it.
 */
public class IiaIndexBasicValidationSuiteV6 extends IiaIndexBasicValidationSuiteV2 {

  private static final Logger logger =
      LoggerFactory.getLogger(IiaIndexBasicValidationSuiteV6.class);
  private static final ValidatedApiInfo apiInfo = new IiaIndexValidatedApiInfoV6();

  @Override
  protected Logger getLogger() {
    return logger;
  }

  @Override
  public ValidatedApiInfo getApiInfo() {
    return apiInfo;
  }

  public IiaIndexBasicValidationSuiteV6(ApiValidator<IiaSuiteState> validator,
                                        IiaSuiteState state, ValidationSuiteConfig config) {
    super(validator, state, config);
  }
}
