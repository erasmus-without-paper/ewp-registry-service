package eu.erasmuswithoutpaper.registry.validators.echovalidator;

import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.CombEntry;
import eu.erasmuswithoutpaper.registry.validators.Combination;
import eu.erasmuswithoutpaper.registry.validators.ValidatedApiInfo;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EchoSetupValidationSuiteV1 extends EchoSetupValidationSuite {
  private static final Logger logger = LoggerFactory.getLogger(EchoSetupValidationSuiteV1.class);

  private static final ValidatedApiInfo apiInfo = new EchoValidatedApiInfoV1();

  @Override
  protected Logger getLogger() {
    return logger;
  }

  @Override
  public ValidatedApiInfo getApiInfo() {
    return apiInfo;
  }

  protected EchoSetupValidationSuiteV1(
      ApiValidator<EchoSuiteState> echoValidator,
      EchoSuiteState state,
      ValidationSuiteConfig config) {
    super(echoValidator, state, config);
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
