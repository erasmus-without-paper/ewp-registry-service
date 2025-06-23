package eu.erasmuswithoutpaper.registry.validators.iiavalidator;

import java.util.Collections;
import java.util.List;

import eu.erasmuswithoutpaper.registry.validators.AbstractSetupValidationSuite;
import eu.erasmuswithoutpaper.registry.validators.ApiEndpoint;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.HttpSecurityDescription;
import eu.erasmuswithoutpaper.registry.validators.ValidatedApiInfo;
import eu.erasmuswithoutpaper.registry.validators.ValidationParameter;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class IiaIndexBasicSetupValidationSuiteV6
    extends AbstractSetupValidationSuite<IiaSuiteState> {

  @Override
  protected ValidatedApiInfo createApiInfo(int version) {
    return new IiaValidatedApiInfo(version, ApiEndpoint.INDEX);
  }

  public static final String HEI_ID_PARAMETER = "hei_id";

  /**
   * Returns parameters used for validating IIAs v6 Index.
   */
  public static List<ValidationParameter> getParameters() {
    return Collections.singletonList(
        new ValidationParameter(HEI_ID_PARAMETER)
    );
  }

  /**
   * Creates a validation suite for IIAs v6 Index endpoint.
   */
  public IiaIndexBasicSetupValidationSuiteV6(ApiValidator<IiaSuiteState> validator,
      IiaSuiteState state, ValidationSuiteConfig config, int version) {
    super(validator, state, config, false, version);
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
    this.currentState.selectedHeiId = getParameterValue(HEI_ID_PARAMETER, this::getSelectedHeiId);
  }
}
