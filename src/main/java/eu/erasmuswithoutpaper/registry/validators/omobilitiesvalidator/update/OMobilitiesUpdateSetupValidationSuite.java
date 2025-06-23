package eu.erasmuswithoutpaper.registry.validators.omobilitiesvalidator.update;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import eu.erasmuswithoutpaper.registry.internet.Request;
import eu.erasmuswithoutpaper.registry.internet.Response;
import eu.erasmuswithoutpaper.registry.validators.AbstractSetupValidationSuite;
import eu.erasmuswithoutpaper.registry.validators.ApiEndpoint;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.HttpSecurityDescription;
import eu.erasmuswithoutpaper.registry.validators.InlineValidationStep;
import eu.erasmuswithoutpaper.registry.validators.ValidatedApiInfo;
import eu.erasmuswithoutpaper.registry.validators.ValidationParameter;
import eu.erasmuswithoutpaper.registry.validators.omobilitiesvalidator.OMobilitiesSuiteState;
import eu.erasmuswithoutpaper.registry.validators.omobilitiesvalidator.OMobilitiesValidatedApiInfo;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

class OMobilitiesUpdateSetupValidationSuite
    extends AbstractSetupValidationSuite<OMobilitiesSuiteState> {

  private final ValidatedApiInfo apiInfo;
  private static final String OMOBILITY_ID_PARAMETER = "omobility_id";
  private static final String LATEST_PROPOSAL_ID_PARAMETER = "latest_proposal_id";

  OMobilitiesUpdateSetupValidationSuite(ApiValidator<OMobilitiesSuiteState> validator,
      OMobilitiesSuiteState state, ValidationSuiteConfig config, int version) {
    super(validator, state, config, false);

    this.apiInfo = new OMobilitiesValidatedApiInfo(version, ApiEndpoint.UPDATE);
  }

  public static List<ValidationParameter> getParameters() {
    return Arrays.asList(
        new ValidationParameter(OMOBILITY_ID_PARAMETER),
        new ValidationParameter(LATEST_PROPOSAL_ID_PARAMETER)
            .dependsOn(OMOBILITY_ID_PARAMETER)
    );
  }

  @Override
  public ValidatedApiInfo getApiInfo() {
    return apiInfo;
  }

  @Override
  protected boolean shouldAnonymousClientBeAllowedToAccessThisApi() {
    return false;
  }

  //FindBugs is not smart enough to infer that actual type of this.currentState
  //is OMobilitiesSuiteState not just SuiteState
  @Override
  @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
  protected void runApiSpecificTests(HttpSecurityDescription securityDescription)
      throws SuiteBroken {
    this.currentState.sendingHeiId = getSelectedHeiId();
    this.currentState.omobilityId = getParameterValue(OMOBILITY_ID_PARAMETER,
        () -> getOmobilityId(securityDescription));
    this.currentState.proposalId = getParameterValue(LATEST_PROPOSAL_ID_PARAMETER,
        () -> getChangesProposalId(this.currentState.omobilityId, this.currentState.sendingHeiId,
            securityDescription));
  }

  private String getChangesProposalId(String omobilityId, String sendingHeiId,
      HttpSecurityDescription securityDescription) throws SuiteBroken {
    final String getUrl = getApiUrlForHei(
        sendingHeiId, this.getApiInfo().getApiName(), ApiEndpoint.GET,
        "Retrieving 'get' endpoint url from catalogue.",
        "Couldn't find 'get' endpoint url in the catalogue. Is manifest correct?");

    final StringBuilder stringBuilder = new StringBuilder();

    this.setup(new InlineValidationStep() {
      @Override
      public String getName() {
        return "Find latest proposal-id to work with.";
      }

      @Override
      @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
      protected Optional<Response> innerRun() throws Failure {
        try {
          Response response = makeRequest(
              this,
              makeApiRequestWithPreferredSecurity(this, getUrl, ApiEndpoint.GET,
                  securityDescription,
                  new ParameterList(new Parameter("omobility_id", omobilityId)))
          );
          expect200(response);
          List<String> selectedStrings = selectFromDocument(
              makeXmlFromBytes(response.getBody()),
              "/omobilities-get-response/student-mobility/nomination/@proposal-id"
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

        String error = "We've tried to get proposal-id attribute for selected omobility-id,"
                + " but get endpoint didn't return it.";

        throw new Failure(error, Status.NOTICE, null);
      }
    });

    return stringBuilder.toString();
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
            new HeiIdAndUrl(null, this.currentState.sendingHeiId, indexUrl, ApiEndpoint.INDEX)),
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
        "/omobilities-index-response/omobility-id",
        "Find mobility to work with.",
        "We tried to find omobility-id to perform tests on, but index endpoint doesn't report "
            + "any omobility-id, cannot continue tests."
    );
  }

  @Override
  protected Request makeApiRequestWithPreferredSecurity(InlineValidationStep step,
      HeiIdAndUrl heiIdAndUrl, HttpSecurityDescription preferredSecurityDescription) {
    return makeApiRequestWithPreferredSecurity(step, heiIdAndUrl.url, heiIdAndUrl.endpoint,
        preferredSecurityDescription, new ParameterList());
  }
}
