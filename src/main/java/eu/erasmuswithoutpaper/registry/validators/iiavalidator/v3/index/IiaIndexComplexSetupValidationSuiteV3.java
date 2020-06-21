package eu.erasmuswithoutpaper.registry.validators.iiavalidator.v3.index;

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
import eu.erasmuswithoutpaper.registry.validators.iiavalidator.IiaSuiteState;
import eu.erasmuswithoutpaper.registry.validators.iiavalidator.v2.index.IiaIndexComplexSetupValidationSuiteV2;
import eu.erasmuswithoutpaper.registry.validators.types.IiasGetResponseV3;
import eu.erasmuswithoutpaper.registry.validators.types.MobilitySpecificationV3;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

public class IiaIndexComplexSetupValidationSuiteV3 extends IiaIndexComplexSetupValidationSuiteV2 {

  private static final Logger logger =
      LoggerFactory.getLogger(
          IiaIndexComplexSetupValidationSuiteV3.class);

  @Override
  protected Logger getLogger() {
    return logger;
  }

  public IiaIndexComplexSetupValidationSuiteV3(
      ApiValidator<IiaSuiteState> validator,
      IiaSuiteState state,
      ValidationSuiteConfig config) {
    super(validator, state, config);
  }

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
          response = IiaIndexComplexSetupValidationSuiteV3.this.internet.makeRequest(request,
              IiaIndexComplexSetupValidationSuiteV3.this.timeoutMillis);
        } catch (SocketTimeoutException e) {
          throw new Failure("Request to 'get' endpoint timed out.",
              Status.ERROR, true);
        } catch (IOException ignored) {
          throw new Failure("Internal error: couldn't perform request to 'get' endpoint.",
              Status.ERROR, null);
        }
        expect200(response);

        IiasGetResponseV3 getResponse;
        try {
          JAXBContext jaxbContext = JAXBContext.newInstance(IiasGetResponseV3.class);
          Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
          Element xml = makeXmlFromBytes(response.getBody(), true);
          getResponse = (IiasGetResponseV3) unmarshaller.unmarshal(xml);
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

        IiasGetResponseV3.Iia iia = getResponse.getIia().get(0);

        // Schema ensures that there are at least two partners in every iia element.
        iiaInfo.heiId = iia.getPartner().get(0).getHeiId();

        if (!Objects.equals(iiaInfo.heiId,
            IiaIndexComplexSetupValidationSuiteV3.this.currentState.selectedHeiId)) {
          throw new Failure(
              "Received 200 OK but <hei-id> of first <partner> was different than we requested."
                  + "Consult tests for 'get' endpoint.",
              Status.NOTICE, response);
        }

        iiaInfo.partnerHeiId = iia.getPartner().get(1).getHeiId();
        ArrayList<MobilitySpecificationV3> specs = new ArrayList<>();
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
