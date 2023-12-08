package eu.erasmuswithoutpaper.registry.validators.iiasvalidator;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

import eu.erasmuswithoutpaper.registry.common.AcademicYearUtils;
import eu.erasmuswithoutpaper.registry.internet.InternetTestHelpers;
import eu.erasmuswithoutpaper.registry.internet.Request;
import eu.erasmuswithoutpaper.registry.internet.Response;
import eu.erasmuswithoutpaper.registry.validators.ParameterInfo;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

import https.github_com.erasmus_without_paper.ewp_specs_api_iias.blob.stable_v7.endpoints.get_response.IiasGetResponse;
import https.github_com.erasmus_without_paper.ewp_specs_api_iias.blob.stable_v7.endpoints.get_response.IiasGetResponse.Iia;
import https.github_com.erasmus_without_paper.ewp_specs_api_iias.blob.stable_v7.endpoints.get_response.MobilitySpecification;
import https.github_com.erasmus_without_paper.ewp_specs_api_iias.blob.stable_v7.endpoints.get_response.StudentTraineeshipMobilitySpec;
import https.github_com.erasmus_without_paper.ewp_specs_api_iias.blob.stable_v7.endpoints.index_response.IiasIndexResponse;
import https.github_com.erasmus_without_paper.ewp_specs_architecture.blob.stable_v1.common_types.StringWithOptionalLang;
import https.github_com.erasmus_without_paper.ewp_specs_types_contact.tree.stable_v1.Contact;

public class IiasServiceValidV7 extends AbstractIiasService {
  private final List<Iia> iias = new ArrayList<>();

  /**
   * @param indexUrl
   *     The endpoint at which to listen for requests.
   * @param getUrl
   *     The endpoint at which to listen for requests.
   * @param registryClient
   *     Initialized and refreshed {@link RegistryClient} instance.
   */
  public IiasServiceValidV7(String indexUrl, String getUrl, RegistryClient registryClient) {
    super(indexUrl, getUrl, registryClient);
    try {
      fillDataBase();
    } catch (DatatypeConfigurationException ignored) {
    }
  }

  private void fillDataBase() throws DatatypeConfigurationException {
    final String THIS_SERVICE_HEI_ID = "test.hei01.uw.edu.pl";
    final String VALIDATOR_HEI_ID = "validator-hei01.developers.erasmuswithoutpaper.eu";
    final MobilitySpecification.MobilitiesPerYear mobilitiesPerYear = new MobilitySpecification.MobilitiesPerYear();
    mobilitiesPerYear.setValue(BigInteger.valueOf(10));

    Iia iia1 = new Iia();
    Iia.Partner iia1Partner1 = new Iia.Partner();
    iia1Partner1.setHeiId(THIS_SERVICE_HEI_ID);
    iia1Partner1.setOunitId("OUNIT1");
    iia1Partner1.setIiaCode("iia1-partner1-code");
    iia1Partner1.setIiaId("iia1-partner1-id");
    Contact iia1Partner1Contact = new Contact();
    StringWithOptionalLang iia1Partner1ContactName = new StringWithOptionalLang();
    iia1Partner1ContactName.setValue("Test");
    iia1Partner1ContactName.setLang("EN");
    iia1Partner1Contact.getContactName().add(iia1Partner1ContactName);
    iia1Partner1.setSigningContact(iia1Partner1Contact);
    iia1Partner1.setSigningDate(
        DatatypeFactory.newInstance().newXMLGregorianCalendar(2018, 5, 14, 12, 22, 10, 0, 120));
    iia1.getPartner().add(iia1Partner1);

    Iia.Partner iia1Partner2 = new Iia.Partner();
    iia1Partner2.setHeiId(VALIDATOR_HEI_ID);
    iia1Partner2.setOunitId("ounit312");
    iia1Partner2.setIiaCode("iia1-partner2-code");
    iia1Partner2.setIiaId("iia1-partner2-id");
    Contact iia1Partner2Contact = new Contact();
    StringWithOptionalLang iia1Partner2ContactName = new StringWithOptionalLang();
    iia1Partner2ContactName.setValue("Test-test");
    iia1Partner2ContactName.setLang("PL");
    iia1Partner2Contact.getContactName().add(iia1Partner2ContactName);
    iia1Partner2.setSigningContact(iia1Partner2Contact);
    iia1Partner2.setSigningDate(
        DatatypeFactory.newInstance().newXMLGregorianCalendar(2018, 5, 14, 12, 22, 54, 0, 120));
    iia1.getPartner().add(iia1Partner2);

    iia1.setInEffect(true);

    Iia.CooperationConditions cooperationConditions = new Iia.CooperationConditions();
    StudentTraineeshipMobilitySpec studentTraineeshipMobilitySpec =
        new StudentTraineeshipMobilitySpec();
    studentTraineeshipMobilitySpec.setTotalMonthsPerYear(new BigDecimal(6));
    studentTraineeshipMobilitySpec.setMobilitiesPerYear(mobilitiesPerYear);
    studentTraineeshipMobilitySpec.setSendingHeiId(THIS_SERVICE_HEI_ID);
    studentTraineeshipMobilitySpec.setSendingOunitId("ounit-test-1");
    studentTraineeshipMobilitySpec.setReceivingHeiId(VALIDATOR_HEI_ID);
    studentTraineeshipMobilitySpec.setReceivingFirstAcademicYearId("2018/2019");
    studentTraineeshipMobilitySpec.setReceivingLastAcademicYearId("2019/2020");
    cooperationConditions.getStudentTraineeshipMobilitySpec().add(studentTraineeshipMobilitySpec);
    iia1.setCooperationConditions(cooperationConditions);
    iia1.setIiaHash("0fa09d911e23f9eea2721362ff27b1ca197235a174eed543a709278ed039c2b2");

    iias.add(iia1);

    Iia iia2 = new Iia();
    Iia.Partner iia2Partner1 = new Iia.Partner();
    iia2Partner1.setHeiId(THIS_SERVICE_HEI_ID);
    iia2Partner1.setOunitId("OUNIT1");
    iia2Partner1.setIiaCode("iia2-partner1-code");
    iia2Partner1.setIiaId("iia2-partner1-id");
    Contact iia2Partner1Contact = new Contact();
    StringWithOptionalLang iia2Partner1ContactName = new StringWithOptionalLang();
    iia2Partner1ContactName.setValue("Test");
    iia2Partner1ContactName.setLang("EN");
    iia2Partner1Contact.getContactName().add(iia2Partner1ContactName);
    iia2Partner1.setSigningContact(iia2Partner1Contact);
    iia2Partner1.setSigningDate(
        DatatypeFactory.newInstance().newXMLGregorianCalendar(2018, 5, 14, 12, 22, 10, 0, 120));
    iia2.getPartner().add(iia2Partner1);

    Iia.Partner iia2Partner2 = new Iia.Partner();
    iia2Partner2.setHeiId(VALIDATOR_HEI_ID);
    iia2Partner2.setOunitId("ounit312");
    iia2Partner2.setIiaCode("iia2-partner2-code");
    iia2Partner2.setIiaId("iia2-partner2-id");
    Contact iia2Partner2Contact = new Contact();
    StringWithOptionalLang iia2Partner2ContactName = new StringWithOptionalLang();
    iia2Partner2ContactName.setValue("Test-test");
    iia2Partner2ContactName.setLang("PL");
    iia2Partner2Contact.getContactName().add(iia2Partner2ContactName);
    iia2Partner2.setSigningContact(iia2Partner2Contact);
    iia2Partner2.setSigningDate(
        DatatypeFactory.newInstance().newXMLGregorianCalendar(2018, 5, 14, 12, 22, 54, 0, 120));
    iia2.getPartner().add(iia2Partner2);

    iia2.setInEffect(true);

    cooperationConditions = new Iia.CooperationConditions();
    studentTraineeshipMobilitySpec = new StudentTraineeshipMobilitySpec();
    studentTraineeshipMobilitySpec.setTotalMonthsPerYear(new BigDecimal(6));
    studentTraineeshipMobilitySpec.setMobilitiesPerYear(mobilitiesPerYear);
    studentTraineeshipMobilitySpec.setSendingHeiId(THIS_SERVICE_HEI_ID);
    studentTraineeshipMobilitySpec.setSendingOunitId("ounit-test-1");
    studentTraineeshipMobilitySpec.setReceivingHeiId(VALIDATOR_HEI_ID);
    studentTraineeshipMobilitySpec.setReceivingFirstAcademicYearId("2018/2019");
    studentTraineeshipMobilitySpec.setReceivingLastAcademicYearId("2019/2020");
    cooperationConditions.getStudentTraineeshipMobilitySpec().add(studentTraineeshipMobilitySpec);
    iia2.setCooperationConditions(cooperationConditions);

    iias.add(iia2);

    coveredHeiIds.add(THIS_SERVICE_HEI_ID);
    partnersHeiIds.add(VALIDATOR_HEI_ID);
  }

  @Override
  protected Response handleIiasIndexRequest(Request request) {
    try {
      RequestData requestData = new RequestData(request);
      checkRequestMethod(requestData.request);
      extractIndexParams(requestData);
      checkReceivingAcademicYearIds(requestData);
      List<Iia> selectedIias = filterIiasByAcademicYear(iias, requestData);
      selectedIias = filterIiasByModifiedSince(selectedIias, requestData);
      List<String> selectedIiaIds = mapToIds(selectedIias);
      return createIiasIndexResponse(selectedIiaIds);
    } catch (ErrorResponseException e) {
      return e.response;
    }
  }

  private void extractIndexParams(AbstractIiasService.RequestData requestData) throws ErrorResponseException {
    checkParamsEncoding(requestData.request);
    Map<String, List<String>> params = InternetTestHelpers.extractAllParams(requestData.request);

    ParameterInfo receivingAcademicYearId =
            ParameterInfo.readParam(params, "receiving_academic_year_id");
    ParameterInfo modifiedSince = ParameterInfo.readParam(params, "modified_since");

    if (modifiedSince.hasMultiple) {
      errorMultipleModifiedSince(requestData);
    }

    requestData.receivingAcademicYearIds = receivingAcademicYearId.allValues;

    if (modifiedSince.firstValueOrNull != null) {
      requestData.modifiedSince = parseModifiedSince(modifiedSince.firstValueOrNull);
      if (requestData.modifiedSince == null) {
        errorInvalidModifiedSince(requestData);
      }
    }
  }

  @Override
  protected Response handleIiasGetRequest(Request request) {
    try {
      RequestData requestData = new RequestData(request);
      checkRequestMethod(requestData.request);
      extractGetParams(requestData);
      checkIds(requestData);
      List<Iia> selectedIds = filterIiasById(iias, requestData);
      List<Iia> result = new ArrayList<>(selectedIds);
      return createIiasGetResponse(result);
    } catch (ErrorResponseException e) {
      return e.response;
    }
  }

  private List<Iia> filterIiasById(List<Iia> selectedIias, RequestData requestData) {
    List<String> selectedIiaIds = selectedIias.stream()
        .map(i -> i.getPartner().get(0).getIiaId())
        .toList();

    List<Iia> result = new ArrayList<>();
    for (String iiaId : requestData.iiaIds) {
      if (selectedIiaIds.contains(iiaId)) {
        result.add(selectedIias.stream()
            .filter(i -> i.getPartner().get(0).getIiaId().equals(iiaId))
            .findFirst().get());
      }
    }

    return result;
  }

  private List<String> mapToIds(List<Iia> selectedIias) {
    return selectedIias.stream().map(i -> i.getPartner().get(0).getIiaId())
        .collect(Collectors.toList());
  }

  private boolean filterAcademicYear(String academicYear, List<String> requestedAcademicYears) {
    return requestedAcademicYears.contains(academicYear);
  }

  private List<Iia> filterIiasByAcademicYear(List<Iia> iias, RequestData requestData) {
    if (requestData.receivingAcademicYearIds.isEmpty()) {
      return iias;
    }

    List<Iia> result = new ArrayList<>();
    for (Iia iia : iias) {
      ArrayList<MobilitySpecification> specs = new ArrayList<>();
      specs.addAll(iia.getCooperationConditions().getStudentStudiesMobilitySpec());
      specs.addAll(iia.getCooperationConditions().getStudentTraineeshipMobilitySpec());
      specs.addAll(iia.getCooperationConditions().getStaffTeacherMobilitySpec());
      specs.addAll(iia.getCooperationConditions().getStaffTrainingMobilitySpec());
      boolean include =
          specs.stream()
              .flatMap(m -> AcademicYearUtils.getAcademicYearsBetween(
                  m.getReceivingFirstAcademicYearId(), m.getReceivingLastAcademicYearId()).stream())
          .anyMatch(ay -> this.filterAcademicYear(ay, requestData.receivingAcademicYearIds));
      if (include) {
        result.add(iia);
      }
    }
    return result;
  }

  private void extractGetParams(RequestData requestData) throws ErrorResponseException {
    checkParamsEncoding(requestData.request);
    Map<String, List<String>> params = InternetTestHelpers.extractAllParams(requestData.request);

    ParameterInfo iiaId = ParameterInfo.readParam(params, "iia_id");

    requestData.iiaIds = iiaId.allValues;

    if (params.isEmpty()) {
      errorNoParams(requestData);
    }
    if (!iiaId.hasAny) {
      errorNoIds(requestData);
    }

    if (requestData.iiaIds.isEmpty()) {
      //We expect all of above members to have any value even in invalid scenarios.
      throw new NullPointerException();
    }
  }

  private void errorNoIds(RequestData requestData) throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(requestData.request, 400, "No iia_id provided")
    );
  }

  private Response createIiasGetResponse(List<Iia> data) {
    IiasGetResponse response = new IiasGetResponse();
    response.getIia().addAll(data);
    return marshallResponse(200, response);
  }

  private Response createIiasIndexResponse(List<String> data) {
    IiasIndexResponse response = new IiasIndexResponse();
    response.getIiaId().addAll(data);
    return marshallResponse(200, response);
  }
}
