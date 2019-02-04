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

public class EchoSetupValidationSuiteV2 extends EchoSetupValidationSuite {
  private static final Logger logger = LoggerFactory.getLogger(EchoSetupValidationSuiteV2.class);

  protected EchoSetupValidationSuiteV2(
      ApiValidator<EchoSuiteState> echoValidator,
      EwpDocBuilder docBuilder,
      Internet internet, String urlStr,
      RegistryClient regClient,
      ManifestRepository repo,
      EchoSuiteState state) {
    super(echoValidator, docBuilder, internet, urlStr, regClient, repo, state);
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
