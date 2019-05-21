package eu.erasmuswithoutpaper.registry.validators.echovalidator;

import eu.erasmuswithoutpaper.registry.documentbuilder.KnownElement;
import eu.erasmuswithoutpaper.registry.documentbuilder.KnownNamespace;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.CombEntry;
import eu.erasmuswithoutpaper.registry.validators.Combination;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EchoSetupValidationSuiteV1 extends EchoSetupValidationSuite {
  private static final Logger logger = LoggerFactory.getLogger(EchoSetupValidationSuiteV1.class);

  protected EchoSetupValidationSuiteV1(
      ApiValidator<EchoSuiteState> echoValidator,
      EchoSuiteState state,
      ValidationSuiteConfig config) {
    super(echoValidator, state, config);
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
  public String getApiPrefix() {
    return "e1";
  }

  @Override
  public String getApiResponsePrefix() {
    return "er1";
  }

  //FindBugs is not smart enough to infer that actual type of this.currentState
  //is EchoSuiteState not just SuiteState
  @Override
  @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
  protected void validateSecurityMethods() throws SuiteBroken {
    // GATTT, PATTT, GSTTT, PSTTT
    this.currentState.combinations.add(
        new Combination("GET", this.currentState.url, this.currentState.matchedApiEntry,
            CombEntry.CLIAUTH_NONE,
            CombEntry.SRVAUTH_TLSCERT, CombEntry.REQENCR_TLS, CombEntry.RESENCR_TLS
        ));
    this.currentState.combinations.add(
        new Combination("POST", this.currentState.url, this.currentState.matchedApiEntry,
            CombEntry.CLIAUTH_NONE,
            CombEntry.SRVAUTH_TLSCERT, CombEntry.REQENCR_TLS, CombEntry.RESENCR_TLS
        ));
    this.currentState.combinations.add(
        new Combination("GET", this.currentState.url, this.currentState.matchedApiEntry,
            CombEntry.CLIAUTH_TLSCERT_SELFSIGNED, CombEntry.SRVAUTH_TLSCERT, CombEntry.REQENCR_TLS,
            CombEntry.RESENCR_TLS
        ));
    this.currentState.combinations.add(
        new Combination("POST", this.currentState.url, this.currentState.matchedApiEntry,
            CombEntry.CLIAUTH_TLSCERT_SELFSIGNED, CombEntry.SRVAUTH_TLSCERT, CombEntry.REQENCR_TLS,
            CombEntry.RESENCR_TLS
        ));
  }
}