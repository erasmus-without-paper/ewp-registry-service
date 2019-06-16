package eu.erasmuswithoutpaper.registry.validators.echovalidator;

import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.ValidatedApiInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EchoSetupValidationSuiteV2 extends EchoSetupValidationSuite {
  private static final Logger logger = LoggerFactory.getLogger(EchoSetupValidationSuiteV2.class);

  private static final ValidatedApiInfo apiInfo = new EchoValidatedApiInfoV2();

  @Override
  protected Logger getLogger() {
    return logger;
  }

  @Override
  public ValidatedApiInfo getApiInfo() {
    return apiInfo;
  }

  protected EchoSetupValidationSuiteV2(
      ApiValidator<EchoSuiteState> echoValidator,
      EchoSuiteState state,
      ValidationSuiteConfig config) {
    super(echoValidator, state, config);
  }
}
