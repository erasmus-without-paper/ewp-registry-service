package eu.erasmuswithoutpaper.registry.validators.omobilitiesvalidator.index;

import java.util.Collections;
import java.util.List;

import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.HttpSecurityDescription;
import eu.erasmuswithoutpaper.registry.validators.ValidationParameter;
import eu.erasmuswithoutpaper.registry.validators.omobilitiesvalidator.OMobilitiesSuiteState;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class OMobilitiesIndexSetupValidationSuiteV3 extends OMobilitiesIndexSetupValidationSuiteV2 {
  OMobilitiesIndexSetupValidationSuiteV3(ApiValidator<OMobilitiesSuiteState> validator,
      OMobilitiesSuiteState state, ValidationSuiteConfig config, int version) {
    super(validator, state, config, version);
  }

  public static List<ValidationParameter> getParameters() {
    return Collections.emptyList();
  }

  //FindBugs is not smart enough to infer that actual type of this.currentState
  //is OMobilitiesSuiteState not just SuiteState
  @Override
  @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
  protected void runApiSpecificTests(HttpSecurityDescription securityDescription)
          throws SuiteBroken {
    this.currentState.sendingHeiId = getSelectedHeiId();
    this.currentState.notPermittedHeiId = getParameterValue(NOT_PERMITTED_HEI_ID_PARAMETER,
            () -> getOtherRealHeiId(this.currentState.sendingHeiId));
  }

  @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
  protected String getSelectedHeiId() throws SuiteBroken {
    return getCoveredHeiIds(this.currentState.url).get(0);
  }
}
