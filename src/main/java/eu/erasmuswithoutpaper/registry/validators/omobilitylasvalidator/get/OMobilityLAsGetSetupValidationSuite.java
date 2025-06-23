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
import eu.erasmuswithoutpaper.registry.validators.omobilitylasvalidator.OMobilityLAsGetValidatedApiInfo;
import eu.erasmuswithoutpaper.registry.validators.omobilitylasvalidator.OMobilityLAsSuiteState;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

class OMobilityLAsGetSetupValidationSuite
    extends AbstractSetupValidationSuite<OMobilityLAsSuiteState> {

  private final ValidatedApiInfo apiInfo;

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

  OMobilityLAsGetSetupValidationSuite(ApiValidator<OMobilityLAsSuiteState> validator,
      OMobilityLAsSuiteState state, ValidationSuiteConfig config, int version) {
    super(validator, state, config, false);

    this.apiInfo = new OMobilityLAsGetValidatedApiInfo(version, ApiEndpoint.GET);
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
        this.currentState.sendingHeiId, this.getApiInfo().getApiName(), ApiEndpoint.INDEX,
        "Retrieving 'index' endpoint url from catalogue.",
        "Couldn't find 'index' endpoint url in the catalogue. Is manifest correct?");

    HeiIdAndString foundOmobilityId = getCoveredOmobilityIds(
        Collections.singletonList(
            new HeiIdAndUrl(
                "sending_hei_id",
                this.currentState.sendingHeiId,
                indexUrl,
                ApiEndpoint.INDEX
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
