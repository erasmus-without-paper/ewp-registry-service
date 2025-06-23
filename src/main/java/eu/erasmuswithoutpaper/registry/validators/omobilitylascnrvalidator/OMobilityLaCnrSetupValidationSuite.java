package eu.erasmuswithoutpaper.registry.validators.omobilitylascnrvalidator;

import java.util.List;

import eu.erasmuswithoutpaper.registry.validators.AbstractSetupValidationSuite;
import eu.erasmuswithoutpaper.registry.validators.ApiEndpoint;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.HttpSecurityDescription;
import eu.erasmuswithoutpaper.registry.validators.ValidatedApiInfo;
import eu.erasmuswithoutpaper.registry.validators.ValidationParameter;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

class OMobilityLaCnrSetupValidationSuite
    extends AbstractSetupValidationSuite<OMobilityLaCnrSuiteState> {

  private static final String SENDING_HEI_ID_PARAMETER = "sending_hei_id";
  private static final String SENDING_HEI_ID = "validator-hei01.developers.erasmuswithoutpaper.eu";
  private static final String OMOBILITY_ID_PARAMETER = "omobility_id";
  private static final String OMOBILITY_ID = "1";

  @Override
  protected ValidatedApiInfo createApiInfo(int version) {
    return new OMobilityLaCnrValidatedApiInfo(version, ApiEndpoint.NO_ENDPOINT);
  }

  public static List<ValidationParameter> getParameters() {
    return List.of(new ValidationParameter(OMOBILITY_ID_PARAMETER));
  }

  OMobilityLaCnrSetupValidationSuite(ApiValidator<OMobilityLaCnrSuiteState> validator,
      OMobilityLaCnrSuiteState state, ValidationSuiteConfig config, int version) {
    super(validator, state, config, true, version);
  }

  @Override
  protected boolean shouldAnonymousClientBeAllowedToAccessThisApi() {
    return false;
  }

  @Override
  @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
  protected void runApiSpecificTests(HttpSecurityDescription securityDescription)
      throws SuiteBroken {
    this.currentState.sendingHeiId =
        getParameterValue(SENDING_HEI_ID_PARAMETER, () -> SENDING_HEI_ID);
    this.currentState.omobilityId = getParameterValue(OMOBILITY_ID_PARAMETER, () -> OMOBILITY_ID);
  }
}
