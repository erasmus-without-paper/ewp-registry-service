package eu.erasmuswithoutpaper.registry.validators.imobilitiesvalidator.get;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import eu.erasmuswithoutpaper.registry.internet.Response;
import eu.erasmuswithoutpaper.registry.validators.AbstractSetupValidationSuite;
import eu.erasmuswithoutpaper.registry.validators.ApiEndpoint;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.HttpSecurityDescription;
import eu.erasmuswithoutpaper.registry.validators.InlineValidationStep;
import eu.erasmuswithoutpaper.registry.validators.ValidatedApiInfo;
import eu.erasmuswithoutpaper.registry.validators.ValidationParameter;
import eu.erasmuswithoutpaper.registry.validators.imobilitiesvalidator.IMobilitiesSuiteState;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


class IMobilitiesGetSetupValidationSuite
    extends AbstractSetupValidationSuite<IMobilitiesSuiteState> {

  private static final Logger logger =
      LoggerFactory.getLogger(
          IMobilitiesGetSetupValidationSuite.class);

  private final ValidatedApiInfo apiInfo;
  private static final String RECEIVING_HEI_ID_PARAMETER = "receiving_hei_id";
  private static final String OMOBILITY_ID_PARAMETER = "omobility_id";

  IMobilitiesGetSetupValidationSuite(ApiValidator<IMobilitiesSuiteState> validator,
      IMobilitiesSuiteState state, ValidationSuiteConfig config, int version) {
    super(validator, state, config);

    this.apiInfo = new IMobilitiesGetValidatedApiInfo(version, ApiEndpoint.Get);
  }

  public static List<ValidationParameter> getParameters() {
    return Arrays.asList(
        new ValidationParameter(RECEIVING_HEI_ID_PARAMETER),
        new ValidationParameter(OMOBILITY_ID_PARAMETER,
            Collections.singletonList(RECEIVING_HEI_ID_PARAMETER))
    );
  }

  @Override
  protected Logger getLogger() {
    return logger;
  }

  @Override
  public ValidatedApiInfo getApiInfo() {
    return apiInfo;
  }

  private int getMaxOmobilityIds() {
    return getMaxIds("omobility-ids");
  }

  @Override
  protected boolean shouldAnonymousClientBeAllowedToAccessThisApi() {
    return false;
  }

  //FindBugs is not smart enough to infer that actual type of this.currentState
  //is IMobilitiesSuiteState not just SuiteState
  @Override
  @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
  protected void runApiSpecificTests(HttpSecurityDescription securityDescription)
      throws SuiteBroken {
    this.currentState.maxOmobilityIds = getMaxOmobilityIds();
    this.currentState.receivingHeiId = getParameterValue(RECEIVING_HEI_ID_PARAMETER,
        this::getReceivingHeiId);
    this.currentState.omobilityId = getParameterValue(OMOBILITY_ID_PARAMETER);

    this.setup(new InlineValidationStep() {
      @Override
      public String getName() {
        return String.format("Check if %s parameter is provided.", OMOBILITY_ID_PARAMETER);
      }

      @Override
      @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
      protected Optional<Response> innerRun() throws Failure {
        if (IMobilitiesGetSetupValidationSuite.this.currentState.omobilityId == null) {
          throw new Failure(
              "The Validator cannot determine what omobility-id should be used, please provide "
                  + "one as a parameter.",
              Status.NOTICE, null);
        }
        return Optional.empty();
      }
    });

  }

  @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
  private String getReceivingHeiId() throws SuiteBroken {
    return getCoveredHeiIds(this.currentState.url).get(0);
  }
}
