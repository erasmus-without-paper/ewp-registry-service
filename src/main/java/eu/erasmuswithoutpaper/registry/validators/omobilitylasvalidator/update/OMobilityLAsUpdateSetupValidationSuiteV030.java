package eu.erasmuswithoutpaper.registry.validators.omobilitylasvalidator.update;

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
import eu.erasmuswithoutpaper.registry.validators.omobilitylasvalidator.OMobilityLAsSuiteState;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.joox.Match;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


class OMobilityLAsUpdateSetupValidationSuiteV030
    extends AbstractSetupValidationSuite<OMobilityLAsSuiteState> {

  private static final Logger logger =
      LoggerFactory.getLogger(OMobilityLAsUpdateSetupValidationSuiteV030.class);

  private static final ValidatedApiInfo apiInfo = new OMobilityLAsUpdateValidatedApiInfo();
  private static final String SENDING_HEI_ID_PARAMETER = "sending_hei_id";
  private static final String OMOBILITY_ID_PARAMETER = "omobility_id";
  private static final String LATEST_PROPOSAL_ID_PARAMETER = "latest_proposal_id";

  OMobilityLAsUpdateSetupValidationSuiteV030(ApiValidator<OMobilityLAsSuiteState> validator,
      OMobilityLAsSuiteState state, ValidationSuiteConfig config) {
    super(validator, state, config);
  }

  public static List<ValidationParameter> getParameters() {
    return Arrays.asList(
        new ValidationParameter(SENDING_HEI_ID_PARAMETER),
        new ValidationParameter(OMOBILITY_ID_PARAMETER)
            .dependsOn(SENDING_HEI_ID_PARAMETER),
        new ValidationParameter(LATEST_PROPOSAL_ID_PARAMETER)
            .dependsOn(OMOBILITY_ID_PARAMETER)
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

  private boolean supportsUpdateType(String type) {
    Match supportedUpdateTypes = getManifestParameter("supported-update-types");
    return supportedUpdateTypes.xpath(
        String.format("%s:%s", getApiInfo().getApiPrefix(), type)
    ).isNotEmpty();
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
    this.currentState.supportsUpdateComponentsStudiedV1 =
        supportsUpdateType("update-components-studied-v1");
    this.currentState.supportsApproveComponentsStudiedProposalV1 =
        supportsUpdateType("approve-components-studied-proposal-v1");
    this.currentState.sendingHeiId = getParameterValue(SENDING_HEI_ID_PARAMETER,
        this::getReceivingHeiId);
    this.currentState.omobilityId = getParameterValue(OMOBILITY_ID_PARAMETER,
        () -> getOmobilityId(securityDescription));
    this.currentState.latestProposalId = getParameterValue(LATEST_PROPOSAL_ID_PARAMETER,
        () -> getLatestProposalId(this.currentState.omobilityId, this.currentState.sendingHeiId,
            securityDescription));
  }

  private String getLatestProposalId(String omobilityId, String sendingHeiId,
      HttpSecurityDescription securityDescription) throws SuiteBroken {
    final String getUrl = getApiUrlForHei(
        sendingHeiId, this.getApiInfo().getApiName(), ApiEndpoint.Get,
        "Retrieving 'get' endpoint url from catalogue.",
        "Couldn't find 'get' endpoint url in the catalogue. Is manifest correct?");

    final StringBuilder stringBuilder = new StringBuilder();


    this.setup(new InlineValidationStep() {
      @Override
      public String getName() {
        return "Find latest-proposal-id to work with.";
      }

      @Override
      @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
      protected Optional<Response> innerRun() throws Failure {
        try {
          Response response = OMobilityLAsUpdateSetupValidationSuiteV030.this.makeRequest(
              this,
              makeApiRequestWithPreferredSecurity(this, getUrl, ApiEndpoint.Get,
                  securityDescription,
                  new ParameterList(
                      new Parameter(
                          "sending_hei_id",
                          sendingHeiId
                      ),
                      new Parameter(
                          "omobility_id",
                          omobilityId
                      )
                  )
              )
          );
          expect200(response);
          List<String> selectedStrings = selectFromDocument(
              makeXmlFromBytes(response.getBody()),
              "/omobility-las-get-response/la/components-studied/latest-proposal/@id"
          );

          if (!selectedStrings.isEmpty()) {
            stringBuilder.append(selectedStrings.get(0));
            return Optional.empty();
          }
        } catch (Failure e) {
          if (e.isFatal()) {
            throw e;
          }
          // else ignore
        }

        String error = "We've tried to get latest-proposal-id for selected omobility-id, but get "
            + "endpoint didn't return it.";

        throw new Failure(error, Status.NOTICE, null);
      }
    });
    return stringBuilder.toString();
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
        "Find learning agreement to work with.",
        "We tried to find omobility-id to perform tests on, but index endpoint doesn't report "
            + "any omobility-id, cannot continue tests."
    );
  }

}
