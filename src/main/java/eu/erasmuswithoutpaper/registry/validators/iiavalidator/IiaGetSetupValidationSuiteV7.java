package eu.erasmuswithoutpaper.registry.validators.iiavalidator;

import java.util.List;

import eu.erasmuswithoutpaper.registry.internet.Request;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.HttpSecurityDescription;
import eu.erasmuswithoutpaper.registry.validators.InlineValidationStep;
import eu.erasmuswithoutpaper.registry.validators.ValidationParameter;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class IiaGetSetupValidationSuiteV7 extends IiaGetSetupValidationSuiteV6 {

  /**
   * Creates a validation suite for IIAs v7 Get endpoint.
   */
  public IiaGetSetupValidationSuiteV7(ApiValidator<IiaSuiteState> validator, IiaSuiteState state,
      ValidationSuiteConfig config, int version) {
    super(validator, state, config, version);
  }

  /**
   * Returns parameters used for validating IIAs v7 Get.
   */
  public static List<ValidationParameter> getParameters() {
    return List.of(new ValidationParameter(IIA_ID_PARAMETER));
  }

  //FindBugs is not smart enough to infer that actual type of this.currentState
  //is IiaSuiteState not just SuiteState
  @Override
  @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
  protected void runApiSpecificTests(HttpSecurityDescription securityDescription)
      throws SuiteBroken {
    this.currentState.maxIiaIds = getMaxIiaIds();
    this.currentState.selectedHeiId = getSelectedHeiId();
    this.currentState.selectedIiaId = getParameterValue(IIA_ID_PARAMETER,
        () -> getIiaId(securityDescription));
  }

  @Override
  protected Request makeApiRequestWithPreferredSecurity(InlineValidationStep step,
      HeiIdAndUrl heiIdAndUrl, HttpSecurityDescription preferredSecurityDescription) {
    return makeApiRequestWithPreferredSecurity(step, heiIdAndUrl.url, heiIdAndUrl.endpoint,
        preferredSecurityDescription, new ParameterList());
  }
}
