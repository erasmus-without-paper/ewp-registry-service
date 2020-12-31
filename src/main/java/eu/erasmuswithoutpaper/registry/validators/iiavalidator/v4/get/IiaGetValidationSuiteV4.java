package eu.erasmuswithoutpaper.registry.validators.iiavalidator.v4.get;

import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.ValidatedApiInfo;
import eu.erasmuswithoutpaper.registry.validators.iiavalidator.IiaSuiteState;
import eu.erasmuswithoutpaper.registry.validators.iiavalidator.v2.get.IiaGetValidationSuiteV2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Describes the set of test/steps to be run on an IIAs API GET endpoint implementation in order to
 * properly validate it.
 */
public class IiaGetValidationSuiteV4 extends IiaGetValidationSuiteV2 {

  private static final Logger logger = LoggerFactory.getLogger(
      IiaGetValidationSuiteV4.class);

  private static final ValidatedApiInfo apiInfo = new IiaGetValidatedApiInfoV4();

  @Override
  protected Logger getLogger() {
    return logger;
  }

  @Override
  public ValidatedApiInfo getApiInfo() {
    return apiInfo;
  }

  public IiaGetValidationSuiteV4(ApiValidator<IiaSuiteState> validator,
                                 IiaSuiteState state, ValidationSuiteConfig config) {
    super(validator, state, config);
  }
}
