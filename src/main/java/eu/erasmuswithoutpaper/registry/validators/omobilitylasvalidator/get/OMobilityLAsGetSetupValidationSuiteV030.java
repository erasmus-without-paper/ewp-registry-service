package eu.erasmuswithoutpaper.registry.validators.omobilitylasvalidator.get;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import eu.erasmuswithoutpaper.registry.validators.AbstractSetupValidationSuite;
import eu.erasmuswithoutpaper.registry.validators.ApiEndpoint;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.HttpSecurityDescription;
import eu.erasmuswithoutpaper.registry.validators.ValidatedApiInfo;
import eu.erasmuswithoutpaper.registry.validators.ValidationParameter;
import eu.erasmuswithoutpaper.registry.validators.omobilitylasvalidator.OMobilityLAsSuiteState;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


class OMobilityLAsGetSetupValidationSuiteV030
    extends AbstractSetupValidationSuite<OMobilityLAsSuiteState> {

  private static final Logger logger =
      LoggerFactory.getLogger(OMobilityLAsGetSetupValidationSuiteV030.class);

  private static final ValidatedApiInfo apiInfo = new OMobilityLAsGetValidatedApiInfo();

  @Override
  protected Logger getLogger() {
    return logger;
  }

  @Override
  public ValidatedApiInfo getApiInfo() {
    return apiInfo;
  }

  private static final String SENDING_HEI_ID_PARAMETER = "sending_hei_id";
  private static final String OMOBILITY_ID_PARAMETER = "omobility_id";

  public static List<ValidationParameter> getParameters() {
    return Arrays.asList(
        new ValidationParameter(SENDING_HEI_ID_PARAMETER),
        new ValidationParameter(OMOBILITY_ID_PARAMETER,
            Collections.singletonList(SENDING_HEI_ID_PARAMETER))
    );
  }

  private int getMaxOmobilityIds() {
    return getMaxIds("omobility-ids");
  }

  OMobilityLAsGetSetupValidationSuiteV030(ApiValidator<OMobilityLAsSuiteState> validator,
      OMobilityLAsSuiteState state, ValidationSuiteConfig config) {
    super(validator, state, config);
  }

  @Override
  protected boolean shouldAnonymousClientBeAllowedToAccessThisApi() {
    return false;
  }

  //FindBugs is not smart enough to infer that actual type of this.currentState
  //is OMobilityLAsSuiteState not just SuiteState
  @Override
  @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
  protected void runApiSpecificTests(HttpSecurityDescription securityDescription)
      throws SuiteBroken {
    this.currentState.maxOmobilityIds = getMaxOmobilityIds();
    this.currentState.sendingHeiId = getParameterValue(SENDING_HEI_ID_PARAMETER,
        this::getReceivingHeiId);
    this.currentState.omobilityId = getParameterValue(OMOBILITY_ID_PARAMETER,
        () -> getOmobilityId(securityDescription));
  }

  @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
  private String getReceivingHeiId() throws SuiteBroken {
    return getCoveredHeiIds(this.currentState.url).get(0);
  }

  @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
  private String getOmobilityId(
      HttpSecurityDescription securityDescription) throws SuiteBroken {

    String indexUrl = getApiUrlForHei(
        this.currentState.sendingHeiId, this.getApiInfo().getApiName(), ApiEndpoint.Index,
        "Retrieving 'index' endpoint url from catalogue.",
        "Couldn't find 'index' endpoint url in the catalogue. Is manifest correct?");

    HeiIdAndString foundOmobilityId = getCoveredOmobilityIds(
        Collections.singletonList(
            new HeiIdAndUrl(
                "sending_hei_id",
                this.currentState.sendingHeiId,
                indexUrl,
                ApiEndpoint.Index
            )
        ),
        securityDescription
    );
    return foundOmobilityId.string;
  }

  private HeiIdAndString getCoveredOmobilityIds(
      List<HeiIdAndUrl> heiIdAndUrls,
      HttpSecurityDescription securityDescription)
      throws SuiteBroken {
    return findResponseWithString(
        heiIdAndUrls,
        securityDescription,
        "/omobility-las-index-response/omobility-id",
        "Find omobility-id to work with.",
        "We tried to find omobility-id to perform tests on, but index endpoint doesn't report "
            + "any omobility-id, cannot continue tests."
    );
  }

}
