package eu.erasmuswithoutpaper.registry.validators.echovalidator;

import eu.erasmuswithoutpaper.registry.documentbuilder.EwpDocBuilder;
import eu.erasmuswithoutpaper.registry.documentbuilder.KnownElement;
import eu.erasmuswithoutpaper.registry.documentbuilder.KnownNamespace;
import eu.erasmuswithoutpaper.registry.internet.Internet;
import eu.erasmuswithoutpaper.registry.repository.ManifestRepository;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.CombEntry;
import eu.erasmuswithoutpaper.registry.validators.Combination;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Describes the set of test/steps to be run on an Echo API implementation in order to properly
 * validate it.
 */
class EchoValidationSuiteV100 extends EchoValidationSuiteBase {
  private static final Logger logger = LoggerFactory.getLogger(EchoValidationSuiteV100.class);

  EchoValidationSuiteV100(ApiValidator echoValidator, EwpDocBuilder docBuilder, Internet internet,
      String urlStr, RegistryClient regClient, ManifestRepository repo) {
    super(echoValidator, docBuilder, internet, urlStr, regClient, repo);
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

  @Override
  protected void validateSecurityMethods() throws SuiteBroken {
    // GATTT, PATTT, GSTTT, PSTTT
    this.combinationsToValidate.add(
        new Combination("GET", this.urlToBeValidated, this.matchedApiEntry, CombEntry.CLIAUTH_NONE,
            CombEntry.SRVAUTH_TLSCERT, CombEntry.REQENCR_TLS, CombEntry.RESENCR_TLS));
    this.combinationsToValidate.add(
        new Combination("POST", this.urlToBeValidated, this.matchedApiEntry, CombEntry.CLIAUTH_NONE,
            CombEntry.SRVAUTH_TLSCERT, CombEntry.REQENCR_TLS, CombEntry.RESENCR_TLS));
    this.combinationsToValidate.add(
        new Combination("GET", this.urlToBeValidated, this.matchedApiEntry,
            CombEntry.CLIAUTH_TLSCERT_SELFSIGNED, CombEntry.SRVAUTH_TLSCERT, CombEntry.REQENCR_TLS,
            CombEntry.RESENCR_TLS));
    this.combinationsToValidate.add(
        new Combination("POST", this.urlToBeValidated, this.matchedApiEntry,
            CombEntry.CLIAUTH_TLSCERT_SELFSIGNED, CombEntry.SRVAUTH_TLSCERT, CombEntry.REQENCR_TLS,
            CombEntry.RESENCR_TLS));
  }
}
