package eu.erasmuswithoutpaper.registry.validators.iiavalidator;

import java.util.ArrayList;
import java.util.stream.Collectors;

import eu.erasmuswithoutpaper.registry.common.AcademicYearUtils;
import eu.erasmuswithoutpaper.registry.internet.Request;
import eu.erasmuswithoutpaper.registry.internet.Response;
import eu.erasmuswithoutpaper.registry.validators.ApiEndpoint;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.HttpSecurityDescription;
import eu.erasmuswithoutpaper.registry.validators.InlineValidationStep;
import eu.erasmuswithoutpaper.registry.validators.InlineValidationStep.Failure;

import https.github_com.erasmus_without_paper.ewp_specs_api_iias.blob.stable_v7.endpoints.get_response.IiasGetResponse;
import https.github_com.erasmus_without_paper.ewp_specs_api_iias.blob.stable_v7.endpoints.get_response.MobilitySpecification;
import jakarta.xml.bind.JAXBException;

public class IiaIndexComplexSetupValidationSuiteV7 extends IiaIndexComplexSetupValidationSuiteV6 {

  public IiaIndexComplexSetupValidationSuiteV7(ApiValidator<IiaSuiteState> validator,
      IiaSuiteState state, ValidationSuiteConfig config, int version) {
    super(validator, state, config, version);
  }

  @Override
  protected void setIiaInfo(IiaSuiteState.IiaInfo iiaInfo, Response response)
      throws JAXBException, Failure {
    IiasGetResponse getResponse = getIiasGetResponse(response, IiasGetResponse.class);

    if (getResponse.getIia().isEmpty()) {
      handleIiaMissing(response);
    }

    IiasGetResponse.Iia iia = getResponse.getIia().get(0);

    iiaInfo.heiId = iia.getPartner().get(0).getHeiId();

    iiaInfo.partnerHeiId = iia.getPartner().get(1).getHeiId();
    ArrayList<MobilitySpecification> specs = new ArrayList<>();
    specs.addAll(iia.getCooperationConditions().getStudentStudiesMobilitySpec());
    specs.addAll(iia.getCooperationConditions().getStudentTraineeshipMobilitySpec());
    specs.addAll(iia.getCooperationConditions().getStaffTeacherMobilitySpec());
    specs.addAll(iia.getCooperationConditions().getStaffTrainingMobilitySpec());

    iiaInfo.receivingAcademicYears =
        specs.stream()
            .flatMap(ms -> AcademicYearUtils.getAcademicYearsBetween(
                ms.getReceivingFirstAcademicYearId(), ms.getReceivingLastAcademicYearId()).stream())
            .distinct().collect(Collectors.toList());
  }

  @Override
  protected Request getGetRequest(InlineValidationStep step, String heiId, String url,
      HttpSecurityDescription securityDescription, String iiaId) {
    return makeApiRequestWithPreferredSecurity(step, url, ApiEndpoint.GET, securityDescription,
        new ParameterList(new Parameter("iia_id", iiaId)));
  }

  @Override
  protected Request makeApiRequestWithPreferredSecurity(InlineValidationStep step,
      HeiIdAndUrl heiIdAndUrl, HttpSecurityDescription preferredSecurityDescription) {
    return makeApiRequestWithPreferredSecurity(step, heiIdAndUrl.url, heiIdAndUrl.endpoint,
        preferredSecurityDescription, new ParameterList());
  }
}
