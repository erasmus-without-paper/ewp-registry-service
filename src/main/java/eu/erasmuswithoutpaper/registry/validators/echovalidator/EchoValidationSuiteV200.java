package eu.erasmuswithoutpaper.registry.validators.echovalidator;

import eu.erasmuswithoutpaper.registry.documentbuilder.KnownElement;
import eu.erasmuswithoutpaper.registry.documentbuilder.KnownNamespace;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Describes the set of test/steps to be run on an Echo API implementation in order to properly
 * validate it.
 */
class EchoValidationSuiteV200 extends EchoValidationSuiteCommon {
  private static final Logger logger = LoggerFactory.getLogger(EchoValidationSuiteV200.class);

  EchoValidationSuiteV200(ApiValidator<EchoSuiteState> echoValidator,
      EchoSuiteState state, ValidationSuiteConfig config) {
    super(echoValidator, state, config);
  }

  @Override
  protected Logger getLogger() {
    return logger;
  }

  @Override
  protected KnownElement getKnownElement() {
    return KnownElement.RESPONSE_ECHO_V2;
  }

  @Override
  protected String getApiNamespace() {
    return KnownNamespace.APIENTRY_ECHO_V2.getNamespaceUri();
  }

  @Override
  protected String getApiVersion() {
    return "2.0.0";
  }

  @Override
  public String getApiPrefix() {
    return "e2";
  }

  @Override
  public String getApiResponsePrefix() {
    return "er2";
  }
}
