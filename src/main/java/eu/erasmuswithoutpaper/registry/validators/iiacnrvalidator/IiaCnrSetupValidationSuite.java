package eu.erasmuswithoutpaper.registry.validators.iiacnrvalidator;

import java.util.List;

import eu.erasmuswithoutpaper.registry.validators.AbstractSetupValidationSuite;
import eu.erasmuswithoutpaper.registry.validators.ApiEndpoint;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.HttpSecurityDescription;
import eu.erasmuswithoutpaper.registry.validators.ValidatedApiInfo;
import eu.erasmuswithoutpaper.registry.validators.ValidationParameter;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

class IiaCnrSetupValidationSuite
    extends AbstractSetupValidationSuite<IiaCnrSuiteState> {

  private final ValidatedApiInfo apiInfo;

  @Override
  public ValidatedApiInfo getApiInfo() {
    return apiInfo;
  }

  private static final String IIA_ID_PARAMETER = "iia_id";
  private static final String IIA_ID = "1";

  public static List<ValidationParameter> getParameters() {
    return List.of(new ValidationParameter(IIA_ID_PARAMETER));
  }

  IiaCnrSetupValidationSuite(ApiValidator<IiaCnrSuiteState> validator, IiaCnrSuiteState state,
      ValidationSuiteConfig config, int version) {
    super(validator, state, config, true);

    this.apiInfo = new IiaCnrValidatedApiInfo(version, ApiEndpoint.NO_ENDPOINT);
  }

  @Override
  protected boolean shouldAnonymousClientBeAllowedToAccessThisApi() {
    return false;
  }

  @Override
  @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
  protected void runApiSpecificTests(HttpSecurityDescription securityDescription)
      throws SuiteBroken {
    this.currentState.iiaId = getParameterValue(IIA_ID_PARAMETER, () -> IIA_ID);
  }
}
