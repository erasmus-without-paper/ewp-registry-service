package eu.erasmuswithoutpaper.registry.validators.echovalidator;

import eu.erasmuswithoutpaper.registry.documentbuilder.EwpDocBuilder;
import eu.erasmuswithoutpaper.registry.documentbuilder.KnownElement;
import eu.erasmuswithoutpaper.registry.documentbuilder.KnownNamespace;
import eu.erasmuswithoutpaper.registry.internet.Internet;
import eu.erasmuswithoutpaper.registry.repository.ManifestRepository;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Describes the set of test/steps to be run on an Echo API implementation in order to properly
 * validate it.
 */
class EchoValidationSuiteV100 extends EchoValidationSuiteCommon {
  private static final Logger logger = LoggerFactory.getLogger(EchoValidationSuiteV100.class);

  EchoValidationSuiteV100(ApiValidator<EchoSuiteState> echoValidator, EwpDocBuilder docBuilder,
      Internet internet, RegistryClient regClient, ManifestRepository repo,
      EchoSuiteState state) {
    super(echoValidator, docBuilder, internet, regClient, repo, state);
  }

  @Override
  protected Logger getLogger() {
    return logger;
  }

  @Override
  protected KnownElement getKnownElement() {
    return KnownElement.RESPONSE_ECHO_V1;
  }

  @Override
  protected String getApiNamespace() {
    return KnownNamespace.APIENTRY_ECHO_V1.getNamespaceUri();
  }

  @Override
  protected String getApiVersion() {
    return "1.0.0";
  }

  @Override
  public String getApiPrefix() {
    return "e1";
  }

  @Override
  public String getApiResponsePrefix() {
    return "er1";
  }
}
