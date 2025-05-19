package eu.erasmuswithoutpaper.registry.validators.omobilitiesvalidator.index;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import eu.erasmuswithoutpaper.registry.internet.Request;
import eu.erasmuswithoutpaper.registry.internet.Response;
import eu.erasmuswithoutpaper.registry.validators.ApiEndpoint;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.HttpSecurityDescription;
import eu.erasmuswithoutpaper.registry.validators.InlineValidationStep;
import eu.erasmuswithoutpaper.registry.validators.ValidationParameter;
import eu.erasmuswithoutpaper.registry.validators.omobilitiesvalidator.OMobilitiesSuiteState;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import https.github_com.erasmus_without_paper.ewp_specs_api_omobilities.blob.stable_v3.endpoints.get_response.OmobilitiesGetResponse;
import https.github_com.erasmus_without_paper.ewp_specs_api_omobilities.blob.stable_v3.endpoints.get_response.StudentMobility;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import org.w3c.dom.Element;

public class OMobilitiesIndexComplexSetupValidationSuiteV3
    extends OMobilitiesIndexComplexSetupValidationSuiteV2 {
  OMobilitiesIndexComplexSetupValidationSuiteV3(ApiValidator<OMobilitiesSuiteState> validator,
      OMobilitiesSuiteState state, ValidationSuiteConfig config, int version) {
    super(validator, state, config, version);
  }

  /**
   * Get a list of parameters supported by this validator.
   *
   * @return list of supported parameters with dependencies.
   */
  public static List<ValidationParameter> getParameters() {
    return Collections.singletonList(new ValidationParameter(OMOBILITY_ID_PARAMETER)
        .withDescription(OMOBILITY_ID_PARAMETER_DESCRIPTION));
  }

  @Override
  protected String getOMobilityReceivingAcademicYearId(String heiId, String omobilityId,
      String getEndpointUrl, HttpSecurityDescription securityDescription) throws SuiteBroken {
    final List<String> receivedAcademicYearIds = new ArrayList<>();
    this.setup(new InlineValidationStep() {
      @Override
      public String getName() {
        return "Use 'get' endpoint to retrieve info about selected OMobility.";
      }

      @Override
      @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
      protected Optional<Response> innerRun() throws Failure {
        Request request = makeApiRequestWithPreferredSecurity(this, getEndpointUrl, ApiEndpoint.GET,
            securityDescription, new ParameterList(new Parameter("omobility_id", omobilityId)));

        if (request == null) {
          throw new Failure("Couldn't find correct 'get' endpoint url in catalogue.", Status.NOTICE,
              null);
        }

        Response response;
        try {
          response = internet.makeRequest(request, timeoutMillis);
        } catch (SocketTimeoutException e) {
          throw new Failure("Request to 'get' endpoint timed out.", Status.ERROR, true);
        } catch (IOException ignored) {
          throw new Failure("Internal error: couldn't perform request to 'get' endpoint.",
              Status.ERROR, null);
        }
        expect200(response);

        OmobilitiesGetResponse getResponse;
        try {
          JAXBContext jaxbContext = JAXBContext.newInstance(OmobilitiesGetResponse.class);
          Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
          Element xml = makeXmlFromBytes(response.getBody(), true);
          getResponse = (OmobilitiesGetResponse) unmarshaller.unmarshal(xml);
        } catch (JAXBException e) {
          throw new Failure("Received 200 OK but the response was empty or didn't contain correct "
              + "get-response. Tests cannot be continued. " + "Consult tests for 'get' endpoint.",
              Status.NOTICE, response);
        }

        if (getResponse.getSingleMobilityObject().isEmpty()) {
          throw new Failure(
              "Received 200 OK but the response did not contain any OMobility, but we requested "
                  + "one. Consult tests for 'get' endpoint.",
              Status.NOTICE, response);
        }

        // the schema requires receiving-academic-year-id element
        StudentMobility mobility = getResponse.getSingleMobilityObject().get(0);
        receivedAcademicYearIds.add(mobility.getNomination().getReceivingAcademicYearId());

        return Optional.empty();
      }
    });

    return receivedAcademicYearIds.get(0);
  }

  @Override
  protected Request makeApiRequestWithPreferredSecurity(InlineValidationStep step,
      HeiIdAndUrl heiIdAndUrl, HttpSecurityDescription preferredSecurityDescription) {
    return makeApiRequestWithPreferredSecurity(step, heiIdAndUrl.url, heiIdAndUrl.endpoint,
        preferredSecurityDescription, new ParameterList());
  }
}
