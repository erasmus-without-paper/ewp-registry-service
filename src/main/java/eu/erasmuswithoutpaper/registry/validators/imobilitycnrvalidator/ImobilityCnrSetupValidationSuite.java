package eu.erasmuswithoutpaper.registry.validators.imobilitycnrvalidator;

import java.util.List;

import eu.erasmuswithoutpaper.registry.validators.AbstractSetupValidationSuite;
import eu.erasmuswithoutpaper.registry.validators.ApiEndpoint;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.HttpSecurityDescription;
import eu.erasmuswithoutpaper.registry.validators.ValidatedApiInfo;
import eu.erasmuswithoutpaper.registry.validators.ValidationParameter;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

class ImobilityCnrSetupValidationSuite
    extends AbstractSetupValidationSuite<ImobilityCnrSuiteState> {

  private final ValidatedApiInfo apiInfo;

  @Override
  public ValidatedApiInfo getApiInfo() {
    return apiInfo;
  }

  private static final String OMOBILITY_ID_PARAMETER = "omobility_id";
  private static final String OMOBILITY_ID = "1";

  public static List<ValidationParameter> getParameters() {
    return List.of(new ValidationParameter(OMOBILITY_ID_PARAMETER));
  }

  ImobilityCnrSetupValidationSuite(ApiValidator<ImobilityCnrSuiteState> validator,
      ImobilityCnrSuiteState state, ValidationSuiteConfig config, int version) {
    super(validator, state, config, true);

    this.apiInfo = new ImobilityCnrValidatedApiInfo(version, ApiEndpoint.NO_ENDPOINT);
  }

  @Override
  protected boolean shouldAnonymousClientBeAllowedToAccessThisApi() {
    return false;
  }

  @Override
  @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
  protected void runApiSpecificTests(HttpSecurityDescription securityDescription)
      throws SuiteBroken {
    this.currentState.omobilityId = getParameterValue(OMOBILITY_ID_PARAMETER, () -> OMOBILITY_ID);
  }
}
