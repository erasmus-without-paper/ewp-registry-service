package eu.erasmuswithoutpaper.registry.validators.omobilitiesvalidator.get;

import java.util.List;

import eu.erasmuswithoutpaper.registry.internet.Request;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.HttpSecurityDescription;
import eu.erasmuswithoutpaper.registry.validators.InlineValidationStep;
import eu.erasmuswithoutpaper.registry.validators.ValidationParameter;
import eu.erasmuswithoutpaper.registry.validators.omobilitiesvalidator.OMobilitiesSuiteState;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class OMobilitiesGetSetupValidationSuiteV3 extends OMobilitiesGetSetupValidationSuiteV2 {
  OMobilitiesGetSetupValidationSuiteV3(ApiValidator<OMobilitiesSuiteState> validator,
      OMobilitiesSuiteState state, ValidationSuiteConfig config, int version) {
    super(validator, state, config, version);
  }

  public static List<ValidationParameter> getParameters() {
    return List.of(new ValidationParameter(OMOBILITY_ID_PARAMETER));
  }

  // FindBugs is not smart enough to infer that actual type of this.currentState
  // is IiaSuiteState not just SuiteState
  @Override
  @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
  protected void runApiSpecificTests(HttpSecurityDescription securityDescription)
      throws SuiteBroken {
    this.currentState.maxOmobilityIds = getMaxOmobilityIds();
    this.currentState.sendingHeiId = getSelectedHeiId();
    this.currentState.omobilityId =
        getParameterValue(OMOBILITY_ID_PARAMETER, () -> getOmobilityId(securityDescription));
  }

  @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
  protected String getSelectedHeiId() throws SuiteBroken {
    return getCoveredHeiIds(this.currentState.url).get(0);
  }

  @Override
  protected Request makeApiRequestWithPreferredSecurity(InlineValidationStep step,
      HeiIdAndUrl heiIdAndUrl, HttpSecurityDescription preferredSecurityDescription) {
    return makeApiRequestWithPreferredSecurity(step, heiIdAndUrl.url, heiIdAndUrl.endpoint,
        preferredSecurityDescription, new ParameterList());
  }
}
