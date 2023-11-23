package eu.erasmuswithoutpaper.registry.validators.omobilitiesvalidator.index;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
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
import https.github_com.erasmus_without_paper.ewp_specs_api_omobilities.blob.stable_v2.endpoints.get_response.OmobilitiesGetResponse;
import https.github_com.erasmus_without_paper.ewp_specs_api_omobilities.blob.stable_v2.endpoints.get_response.StudentMobility;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

public class OMobilitiesIndexComplexSetupValidationSuite
    extends AbstractSetupValidationSuite<OMobilitiesSuiteState> {

  private static final Logger logger =
      LoggerFactory.getLogger(
          OMobilitiesIndexComplexSetupValidationSuite.class);

  private final ValidatedApiInfo apiInfo;

  @Override
  protected Logger getLogger() {
    return logger;
  }

  private static final String SENDING_HEI_ID_PARAMETER = "sending_hei_id";
  private static final String OMOBILITY_ID_PARAMETER = "omobility_id";
  private static final String OMOBILITY_ID_PARAMETER_DESCRIPTION =
      "This parameter is used to fetch receiving_academic_year_id using GET endpoint.";

  /**
   * Get list of parameters supported by this validator.
   *
   * @return list of supported parameters with dependeies.
   */
  public static List<ValidationParameter> getParameters() {
    return Arrays.asList(
        new ValidationParameter(OMOBILITY_ID_PARAMETER)
            .dependsOn(SENDING_HEI_ID_PARAMETER)
            .withDescription(OMOBILITY_ID_PARAMETER_DESCRIPTION)
    );
  }

  OMobilitiesIndexComplexSetupValidationSuite(
      ApiValidator<OMobilitiesSuiteState> validator,
      OMobilitiesSuiteState state,
      ValidationSuiteConfig config,
      int version) {
    super(validator, state, config);

    this.apiInfo = new OMobilitiesValidatedApiInfo(version, ApiEndpoint.INDEX);
  }

  // Overriding runTests to skip checking url, api version etc.
  @Override
  protected void runTests(HttpSecurityDescription security) throws SuiteBroken {
    runApiSpecificTests(security);
  }

  @Override
  public ValidatedApiInfo getApiInfo() {
    return apiInfo;
  }

  //FindBugs is not smart enough to infer that actual type of this.currentState
  //is OMobilitiesSuiteState not just SuiteState
  @Override
  @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
  protected void runApiSpecificTests(
      HttpSecurityDescription securityDescription) throws SuiteBroken {
    this.currentState.omobilityId = getParameterValue(OMOBILITY_ID_PARAMETER,
        () -> getOMobilitiesIdParameter(securityDescription));
    String getUrl = getApiUrlForHei(
        this.currentState.sendingHeiId, this.getApiInfo().getApiName(), ApiEndpoint.GET,
        "Retrieving 'get' endpoint url from catalogue.",
        "Couldn't find 'get' endpoint url in the catalogue. Is manifest correct?");

    // Call 'get' endpoint to fetch additional information about one of OMobilities.
    this.currentState.receivingAcademicYearId = getOMobilityReceivingAcademicYearId(
        this.currentState.sendingHeiId,
        this.currentState.omobilityId,
        getUrl,
        securityDescription);
  }

  @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
  protected String getOMobilitiesIdParameter(
      HttpSecurityDescription securityDescription) throws SuiteBroken {
    HeiIdAndString foundOMobilitiesId = getCoveredOMobilitiesIds(
        Collections.singletonList(
            new HeiIdAndUrl(
                "sending_hei_id",
                this.currentState.sendingHeiId,
                this.currentState.url,
                ApiEndpoint.INDEX
            )
        ),
        securityDescription
    );
    return foundOMobilitiesId.string;
  }

  protected String getOMobilityReceivingAcademicYearId(
      String heiId, String omobilityId, String getEndpointUrl,
      HttpSecurityDescription securityDescription
  ) throws SuiteBroken {
    final List<String> receivedAcademicYearIds = new ArrayList<>();
    this.setup(new InlineValidationStep() {
      @Override
      public String getName() {
        return "Use 'get' endpoint to retrieve info about selected OMobility.";
      }

      @Override
      @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
      protected Optional<Response> innerRun() throws Failure {
        Request request = makeApiRequestWithPreferredSecurity(
            this,
            getEndpointUrl, ApiEndpoint.GET, securityDescription,
            new ParameterList(
                new Parameter("sending_hei_id", heiId),
                new Parameter("omobility_id", omobilityId)
            ));

        if (request == null) {
          throw new Failure(
              "Couldn't find correct 'get' endpoint url in catalogue.",
              Status.NOTICE, null);
        }

        Response response;
        try {
          response = internet.makeRequest(request, timeoutMillis);
        } catch (SocketTimeoutException e) {
          throw new Failure("Request to 'get' endpoint timed out.",
              Status.ERROR, true);
        } catch (IOException ignored) {
          throw new Failure("Internal error: couldn't perform request to 'get' endpoint.",
              Status.ERROR, null);
        }
        expect200(response);

        OmobilitiesGetResponse getResponse;
        try {
          JAXBContext jaxbContext = JAXBContext.newInstance(
              OmobilitiesGetResponse.class);
          Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
          Element xml = makeXmlFromBytes(response.getBody(), true);
          getResponse = (OmobilitiesGetResponse) unmarshaller.unmarshal(xml);
        } catch (JAXBException e) {
          throw new Failure(
              "Received 200 OK but the response was empty or didn't contain correct "
                  + "get-response. Tests cannot be continued. "
                  + "Consult tests for 'get' endpoint.",
              Status.NOTICE, response);
        }

        if (getResponse.getSingleMobilityObject().isEmpty()) {
          throw new Failure(
              "Received 200 OK but the response did not contain any OMobility, but we requested "
                  + "one. Consult tests for 'get' endpoint.",
              Status.NOTICE, response);
        }

        // receiving-academic-year-id element is required by the schema.
        StudentMobility mobility =
            getResponse.getSingleMobilityObject().get(0);
        receivedAcademicYearIds.add(mobility.getReceivingAcademicYearId());

        return Optional.empty();
      }
    });

    return receivedAcademicYearIds.get(0);
  }

  private HeiIdAndString getCoveredOMobilitiesIds(
      List<HeiIdAndUrl> heiIdAndUrls,
      HttpSecurityDescription securityDescription)
      throws SuiteBroken {
    return findResponseWithString(
        heiIdAndUrls,
        securityDescription,
        "/omobilities-index-response/omobility-id",
        "Find omobility-id to work with.",
        "We tried to find omobility-id to perform tests on, but index endpoint doesn't report any"
            + " omobility-id, cannot continue tests."
    );
  }
}
