package eu.erasmuswithoutpaper.registry.validators.imobilitytorsvalidator.get;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import eu.erasmuswithoutpaper.registry.validators.AbstractSetupValidationSuite;
import eu.erasmuswithoutpaper.registry.validators.ApiEndpoint;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.HttpSecurityDescription;
import eu.erasmuswithoutpaper.registry.validators.ValidatedApiInfo;
import eu.erasmuswithoutpaper.registry.validators.ValidationParameter;
import eu.erasmuswithoutpaper.registry.validators.imobilitytorsvalidator.IMobilityTorsSuiteState;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


class IMobilityTorsGetSetupValidationSuiteV070
    extends AbstractSetupValidationSuite<IMobilityTorsSuiteState> {

  private static final Logger logger =
      LoggerFactory.getLogger(IMobilityTorsGetSetupValidationSuiteV070.class);

  private static final ValidatedApiInfo apiInfo = new IMobilityTorsGetValidatedApiInfo();

  @Override
  protected Logger getLogger() {
    return logger;
  }

  @Override
  public ValidatedApiInfo getApiInfo() {
    return apiInfo;
  }

  static final String RECEIVING_HEI_ID_PARAMETER = "receiving_hei_id";
  static final String OMOBILITY_ID_PARAMETER = "omobility_id";

  public static List<ValidationParameter> getParameters() {
    return Arrays.asList(
        new ValidationParameter(RECEIVING_HEI_ID_PARAMETER),
        new ValidationParameter(OMOBILITY_ID_PARAMETER,
            Collections.singletonList(RECEIVING_HEI_ID_PARAMETER))
    );
  }

  private int getMaxOmobilityIds() {
    return getMaxIds("omobility-ids");
  }

  IMobilityTorsGetSetupValidationSuiteV070(ApiValidator<IMobilityTorsSuiteState> validator,
      IMobilityTorsSuiteState state, ValidationSuiteConfig config) {
    super(validator, state, config);
  }

  @Override
  protected boolean shouldAnonymousClientBeAllowedToAccessThisApi() {
    return false;
  }

  //FindBugs is not smart enough to infer that actual type of this.currentState
  //is IMobilityTorsSuiteState not just SuiteState
  @Override
  @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
  protected void runApiSpecificTests(HttpSecurityDescription securityDescription)
      throws SuiteBroken {
    if (this.currentState.parameters.contains(RECEIVING_HEI_ID_PARAMETER)) {
      this.currentState.receivingHeiId = this.currentState.parameters
          .get(RECEIVING_HEI_ID_PARAMETER);
    } else {
      this.currentState.receivingHeiId = getReceivingHeiId();
    }

    if (this.currentState.parameters.contains(OMOBILITY_ID_PARAMETER)) {
      this.currentState.omobilityId = this.currentState.parameters.get(OMOBILITY_ID_PARAMETER);
    } else {
      this.currentState.omobilityId = getOmobilityId(securityDescription);
    }

    this.currentState.maxOmobilityIds = getMaxOmobilityIds();
  }

  @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
  private String getReceivingHeiId() throws SuiteBroken {
    return "test.hei01.uw.edu.pl";
  }

  @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
  private String getOmobilityId(
      HttpSecurityDescription securityDescription) throws SuiteBroken {

    String indexUrl = getApiUrlForHei(
        this.currentState.receivingHeiId, this.getApiInfo().getApiName(), ApiEndpoint.Index,
        "Retrieving 'index' endpoint url from catalogue.",
        "Couldn't find 'index' endpoint url in the catalogue. Is manifest correct?");

    HeiIdAndString foundOmobilityId = getCoveredOmobilityIds(
        Collections.singletonList(
            new HeiIdAndUrl(
                "receiving_hei_id",
                this.currentState.receivingHeiId,
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
        "/imobility-tors-index-response/omobility-id",
        "Find omobility-id to work with.",
        "We tried to find omobility-id to perform tests on, but index endpoint doesn't report "
            + "any omobility-id, cannot continue tests."
    );
  }

}
