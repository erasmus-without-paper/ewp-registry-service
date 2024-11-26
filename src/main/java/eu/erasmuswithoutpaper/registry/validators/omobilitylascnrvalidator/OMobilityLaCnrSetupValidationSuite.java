package eu.erasmuswithoutpaper.registry.validators.omobilitylascnrvalidator;

import java.util.List;

import eu.erasmuswithoutpaper.registry.validators.AbstractSetupValidationSuite;
import eu.erasmuswithoutpaper.registry.validators.ApiEndpoint;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.HttpSecurityDescription;
import eu.erasmuswithoutpaper.registry.validators.ValidatedApiInfo;
import eu.erasmuswithoutpaper.registry.validators.ValidationParameter;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class OMobilityLaCnrSetupValidationSuite
    extends AbstractSetupValidationSuite<OMobilityLaCnrSuiteState> {

  private static final Logger logger =
      LoggerFactory.getLogger(OMobilityLaCnrSetupValidationSuite.class);
  private static final String SENDING_HEI_ID_PARAMETER = "sending_hei_id";
  private static final String SENDING_HEI_ID = "validator-hei01.developers.erasmuswithoutpaper.eu";

  private final ValidatedApiInfo apiInfo;

  @Override
  protected Logger getLogger() {
    return logger;
  }

  @Override
  public ValidatedApiInfo getApiInfo() {
    return apiInfo;
  }

  private static final String OMOBILITY_ID_PARAMETER = "omobility_id";

  public static List<ValidationParameter> getParameters() {
    return List.of(new ValidationParameter(OMOBILITY_ID_PARAMETER));
  }

  OMobilityLaCnrSetupValidationSuite(ApiValidator<OMobilityLaCnrSuiteState> validator,
      OMobilityLaCnrSuiteState state, ValidationSuiteConfig config, int version) {
    super(validator, state, config, true);

    this.apiInfo = new OMobilityLaCnrValidatedApiInfo(version, ApiEndpoint.NO_ENDPOINT);
  }

  @Override
  @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
  protected void runApiSpecificTests(HttpSecurityDescription securityDescription)
      throws SuiteBroken {
    this.currentState.sendingHeiId =
        getParameterValue(SENDING_HEI_ID_PARAMETER, () -> SENDING_HEI_ID);
  }
}
