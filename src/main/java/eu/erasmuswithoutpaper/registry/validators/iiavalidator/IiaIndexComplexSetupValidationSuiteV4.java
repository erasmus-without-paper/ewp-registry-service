package eu.erasmuswithoutpaper.registry.validators.iiavalidator;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import eu.erasmuswithoutpaper.registry.internet.Request;
import eu.erasmuswithoutpaper.registry.internet.Response;
import eu.erasmuswithoutpaper.registry.validators.ApiEndpoint;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.HttpSecurityDescription;
import eu.erasmuswithoutpaper.registry.validators.InlineValidationStep;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import https.github_com.erasmus_without_paper.ewp_specs_api_iias.blob.stable_v4.endpoints.get_response.IiasGetResponse;
import https.github_com.erasmus_without_paper.ewp_specs_api_iias.blob.stable_v4.endpoints.get_response.MobilitySpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

public class IiaIndexComplexSetupValidationSuiteV4 extends IiaIndexComplexSetupValidationSuiteV2 {

  private static final Logger logger =
      LoggerFactory.getLogger(
          IiaIndexComplexSetupValidationSuiteV4.class);

  @Override
  protected Logger getLogger() {
    return logger;
  }

  public IiaIndexComplexSetupValidationSuiteV4(
      ApiValidator<IiaSuiteState> validator,
      IiaSuiteState state,
      ValidationSuiteConfig config,
      int version) {
    super(validator, state, config, version);
  }

  @Override
  protected IiaSuiteState.IiaInfo getIiaInfo(
      String heiId, String iiaId, String url,
      HttpSecurityDescription securityDescription
  ) throws SuiteBroken {
    IiaSuiteState.IiaInfo iiaInfo = new IiaSuiteState.IiaInfo();
    this.setup(new InlineValidationStep() {
      @Override
      public String getName() {
        return "Use 'get' endpoint to retrieve info about selected IIA.";
      }

      @Override
      @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
      protected Optional<Response> innerRun() throws Failure {
        Request request = makeApiRequestWithPreferredSecurity(
            this,
            url, ApiEndpoint.Get, securityDescription,
            new ParameterList(
                new Parameter("hei_id", heiId),
                new Parameter("iia_id", iiaId)
            ));

        if (request == null) {
          throw new Failure(
              "Couldn't find correct 'get' endpoint url in catalogue.",
              Status.NOTICE, null);
        }

        Response response;
        try {
          response = IiaIndexComplexSetupValidationSuiteV4.this.internet.makeRequest(request,
              IiaIndexComplexSetupValidationSuiteV4.this.timeoutMillis);
        } catch (SocketTimeoutException e) {
          throw new Failure("Request to 'get' endpoint timed out.",
              Status.ERROR, true);
        } catch (IOException ignored) {
          throw new Failure("Internal error: couldn't perform request to 'get' endpoint.",
              Status.ERROR, null);
        }
        expect200(response);

        IiasGetResponse getResponse;
        try {
          JAXBContext jaxbContext = JAXBContext.newInstance(IiasGetResponse.class);
          Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
          Element xml = makeXmlFromBytes(response.getBody(), true);
          getResponse = (IiasGetResponse) unmarshaller.unmarshal(xml);
        } catch (JAXBException e) {
          throw new Failure(
              "Received 200 OK but the response was empty or didn't contain correct "
                  + "get-response. Tests cannot be continued. "
                  + "Consult tests for 'get' endpoint.",
              Status.NOTICE, response);
        }

        if (getResponse.getIia().isEmpty()) {
          throw new Failure(
              "Received 200 OK but the response did not contain any IIA, but we requested one. "
                  + "Consult tests for 'get' endpoint.",
              Status.NOTICE, response);
        }

        IiasGetResponse.Iia iia = getResponse.getIia().get(0);

        // Schema ensures that there are at least two partners in every iia element.
        iiaInfo.heiId = iia.getPartner().get(0).getHeiId();

        if (!Objects.equals(iiaInfo.heiId,
            IiaIndexComplexSetupValidationSuiteV4.this.currentState.selectedHeiId)) {
          throw new Failure(
              "Received 200 OK but <hei-id> of first <partner> was different than we requested."
                  + "Consult tests for 'get' endpoint.",
              Status.NOTICE, response);
        }

        iiaInfo.partnerHeiId = iia.getPartner().get(1).getHeiId();
        ArrayList<MobilitySpecification> specs = new ArrayList<>();
        specs.addAll(iia.getCooperationConditions().getStudentStudiesMobilitySpec());
        specs.addAll(iia.getCooperationConditions().getStudentTraineeshipMobilitySpec());
        specs.addAll(iia.getCooperationConditions().getStaffTeacherMobilitySpec());
        specs.addAll(iia.getCooperationConditions().getStaffTrainingMobilitySpec());

        iiaInfo.receivingAcademicYears = specs.stream()
            .flatMap(ms -> ms.getReceivingAcademicYearId().stream()).distinct()
            .collect(Collectors.toList());

        return Optional.empty();
      }
    });

    return iiaInfo;
  }
}
