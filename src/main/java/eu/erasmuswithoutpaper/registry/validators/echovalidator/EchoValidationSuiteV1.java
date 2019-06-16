package eu.erasmuswithoutpaper.registry.validators.echovalidator;

import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.ValidatedApiInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Describes the set of test/steps to be run on an Echo API implementation in order to properly
 * validate it.
 */
class EchoValidationSuiteV1 extends EchoValidationSuiteCommon {
  private static final Logger logger = LoggerFactory.getLogger(EchoValidationSuiteV1.class);

  private static final ValidatedApiInfo apiInfo = new EchoValidatedApiInfoV1();

  @Override
  protected Logger getLogger() {
    return logger;
  }

  @Override
  public ValidatedApiInfo getApiInfo() {
    return apiInfo;
  }

  EchoValidationSuiteV1(ApiValidator<EchoSuiteState> echoValidator,
      EchoSuiteState state, ValidationSuiteConfig config) {
    super(echoValidator, state, config);
  }
}
