package eu.erasmuswithoutpaper.registry.validators.iiavalidator;

import eu.erasmuswithoutpaper.registry.validators.AbstractSetupValidationSuite;
import eu.erasmuswithoutpaper.registry.validators.ApiEndpoint;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.HttpSecurityDescription;
import eu.erasmuswithoutpaper.registry.validators.ValidatedApiInfo;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class IiaIndexBasicSetupValidationSuiteV7
    extends AbstractSetupValidationSuite<IiaSuiteState> {

  private static final Logger logger =
      LoggerFactory.getLogger(IiaIndexBasicSetupValidationSuiteV7.class);

  private final ValidatedApiInfo apiInfo;

  @Override
  protected Logger getLogger() {
    return logger;
  }

  @Override
  public ValidatedApiInfo getApiInfo() {
    return apiInfo;
  }

  /**
   * Creates a validation suite for IIAs v7 Index endpoint.
   */
  public IiaIndexBasicSetupValidationSuiteV7(ApiValidator<IiaSuiteState> validator,
      IiaSuiteState state, ValidationSuiteConfig config, int version) {
    super(validator, state, config, false);

    this.apiInfo = new IiaValidatedApiInfo(version, ApiEndpoint.INDEX);
  }

  @Override
  protected boolean shouldAnonymousClientBeAllowedToAccessThisApi() {
    return false;
  }

  //FindBugs is not smart enough to infer that actual type of this.currentState
  //is IiaSuiteState not just SuiteState
  @Override
  @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
  protected void runApiSpecificTests(HttpSecurityDescription securityDescription)
      throws SuiteBroken {
    this.currentState.selectedHeiId = getSelectedHeiId();
  }

  @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
  protected String getSelectedHeiId() throws SuiteBroken {
    return getCoveredHeiIds(this.currentState.url).get(0);
  }
}
