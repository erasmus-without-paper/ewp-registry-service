package eu.erasmuswithoutpaper.registry.validators.iiasvalidator;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

import eu.erasmuswithoutpaper.registry.internet.InternetTestHelpers;
import eu.erasmuswithoutpaper.registry.internet.Request;
import eu.erasmuswithoutpaper.registry.internet.Response;
import eu.erasmuswithoutpaper.registry.validators.ParameterInfo;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

import https.github_com.erasmus_without_paper.ewp_specs_api_iias.blob.stable_v4.endpoints.get_response.IiasGetResponse;
import https.github_com.erasmus_without_paper.ewp_specs_api_iias.blob.stable_v4.endpoints.get_response.IiasGetResponse.Iia;
import https.github_com.erasmus_without_paper.ewp_specs_api_iias.blob.stable_v4.endpoints.get_response.MobilitySpecification;
import https.github_com.erasmus_without_paper.ewp_specs_api_iias.blob.stable_v4.endpoints.get_response.StudentTraineeshipMobilitySpec;
import https.github_com.erasmus_without_paper.ewp_specs_api_iias.blob.stable_v4.endpoints.index_response.IiasIndexResponse;
import https.github_com.erasmus_without_paper.ewp_specs_architecture.blob.stable_v1.common_types.StringWithOptionalLang;
import https.github_com.erasmus_without_paper.ewp_specs_types_contact.tree.stable_v1.Contact;

public class IiasServiceValidV4 extends AbstractIiasService {
  protected List<String> coveredHeiIds = new ArrayList<>();
  protected List<String> partnersHeiIds = new ArrayList<>();
  protected List<Iia> iias = new ArrayList<>();

  /**
   * @param indexUrl
   *     The endpoint at which to listen for requests.
   * @param getUrl
   *     The endpoint at which to listen for requests.
   * @param registryClient
   *     Initialized and refreshed {@link RegistryClient} instance.
   */
  public IiasServiceValidV4(String indexUrl, String getUrl, RegistryClient registryClient) {
    super(indexUrl, getUrl, registryClient);
    try {
      fillDataBase();
    } catch (DatatypeConfigurationException ignored) {
    }
  }

  protected void fillDataBase() throws DatatypeConfigurationException {
    final String THIS_SERVICE_HEI_ID = "test.hei01.uw.edu.pl";
    final String VALIDATOR_HEI_ID = "validator-hei01.developers.erasmuswithoutpaper.eu";

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

    final String OTHER_HEI_ID = "other hei id";
    Iia.Partner iia1Partner3 = new Iia.Partner();
    iia1Partner3.setHeiId(OTHER_HEI_ID);
    iia1Partner3.setOunitId("ounit112");
    iia1Partner3.setIiaCode("iia1-partner3-code");
    iia1Partner3.setIiaId("iia1-partner3-id");
    Contact iia1Partner3Contact = new Contact();
    StringWithOptionalLang iia1Partner3ContactName = new StringWithOptionalLang();
    iia1Partner3ContactName.setValue("Test-test-test");
    iia1Partner3ContactName.setLang("DE");
    iia1Partner3Contact.getContactName().add(iia1Partner3ContactName);
    iia1Partner3.setSigningContact(iia1Partner3Contact);
    iia1Partner3.setSigningDate(
        DatatypeFactory.newInstance().newXMLGregorianCalendar(2018, 5, 14, 12, 23, 18, 0, 120));
    iia1.getPartner().add(iia1Partner3);

    iia1.setInEffect(true);

    Iia.CooperationConditions cooperationConditions = new Iia.CooperationConditions();
    StudentTraineeshipMobilitySpec studentTraineeshipMobilitySpec =
        new StudentTraineeshipMobilitySpec();
    studentTraineeshipMobilitySpec.setTotalMonths(new BigDecimal(6));
    studentTraineeshipMobilitySpec.setMobilitiesPerYear(BigInteger.valueOf(10));
    studentTraineeshipMobilitySpec.setSendingHeiId(THIS_SERVICE_HEI_ID);
    studentTraineeshipMobilitySpec.getSendingOunitId().add("ounit-test-1");
    studentTraineeshipMobilitySpec.setReceivingHeiId(VALIDATOR_HEI_ID);
    studentTraineeshipMobilitySpec.getReceivingAcademicYearId().add("2018/2019");
    studentTraineeshipMobilitySpec.getReceivingAcademicYearId().add("2019/2020");
    cooperationConditions.getStudentTraineeshipMobilitySpec().add(studentTraineeshipMobilitySpec);
    iia1.setCooperationConditions(cooperationConditions);

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

    Iia.Partner iia2Partner3 = new Iia.Partner();
    iia2Partner3.setHeiId(OTHER_HEI_ID);
    iia2Partner3.setOunitId("ounit112");
    iia2Partner3.setIiaCode("iia2-partner3-code");
    iia2Partner3.setIiaId("iia2-partner3-id");
    Contact iia2Partner3Contact = new Contact();
    StringWithOptionalLang iia2Partner3ContactName = new StringWithOptionalLang();
    iia2Partner3ContactName.setValue("Test-test-test");
    iia2Partner3ContactName.setLang("DE");
    iia2Partner3Contact.getContactName().add(iia2Partner3ContactName);
    iia2Partner3.setSigningContact(iia2Partner3Contact);
    iia2Partner3.setSigningDate(
        DatatypeFactory.newInstance().newXMLGregorianCalendar(2018, 5, 14, 12, 23, 18, 0, 120));
    iia2.getPartner().add(iia2Partner3);

    iia2.setInEffect(true);

    cooperationConditions = new Iia.CooperationConditions();
    studentTraineeshipMobilitySpec = new StudentTraineeshipMobilitySpec();
    studentTraineeshipMobilitySpec.setTotalMonths(new BigDecimal(6));
    studentTraineeshipMobilitySpec.setMobilitiesPerYear(BigInteger.valueOf(10));
    studentTraineeshipMobilitySpec.setSendingHeiId(THIS_SERVICE_HEI_ID);
    studentTraineeshipMobilitySpec.getSendingOunitId().add("ounit-test-1");
    studentTraineeshipMobilitySpec.setReceivingHeiId(VALIDATOR_HEI_ID);
    studentTraineeshipMobilitySpec.getReceivingAcademicYearId().add("2018/2019");
    studentTraineeshipMobilitySpec.getReceivingAcademicYearId().add("2019/2020");
    cooperationConditions.getStudentTraineeshipMobilitySpec().add(studentTraineeshipMobilitySpec);
    iia2.setCooperationConditions(cooperationConditions);

    iias.add(iia2);

    coveredHeiIds.add(THIS_SERVICE_HEI_ID);
    partnersHeiIds.add(VALIDATOR_HEI_ID);
  }

  @Override
  protected Response handleIiasIndexRequest(Request request) throws ErrorResponseException {
    try {
      RequestData requestData = new RequestData(request);
      checkRequestMethod(requestData.request);
      extractIndexParams(requestData);
      checkHei(requestData);
      checkPartnerHei(requestData);
      checkReceivingAcademicYearIds(requestData);
      List<Iia> selectedIias = filterIiasByHeiId(iias, requestData);
      selectedIias = filterIiasByPartnerHeiId(selectedIias, requestData);
      selectedIias = filterIiasByAcademicYear(selectedIias, requestData);
      selectedIias = filterIiasByModifiedSince(selectedIias, requestData);
      List<String> selectedIiaIds = mapToIds(selectedIias);
      return createIiasIndexResponse(selectedIiaIds);
    } catch (ErrorResponseException e) {
      return e.response;
    }
  }

  protected List<Iia> filterIiasByModifiedSince(List<Iia> selectedIias, RequestData requestData) {
    if (requestData.modifiedSince == null) {
      return selectedIias;
    }
    Instant modifiedAt = Instant.from(
        ZonedDateTime.of(2019, 6, 10, 18, 52, 32, 0, ZoneId.of("Z"))
    );
    if (requestData.modifiedSince.toInstant().isAfter(modifiedAt)) {
      return new ArrayList<>();
    } else {
      return selectedIias;
    }
  }

  protected int getMaxIiaCodes() {
    return 3;
  }

  protected int getMaxIiaIds() {
    return 3;
  }

  private void checkCodes(RequestData requestData) throws ErrorResponseException {
    if (requestData.iiaCodes.size() > getMaxIiaCodes()) {
      errorMaxCodesExceeded(requestData);
    }
  }

  private void checkIds(RequestData requestData) throws ErrorResponseException {
    if (requestData.iiaIds.size() > getMaxIiaIds()) {
      errorMaxIdsExceeded(requestData);
    }
  }

  protected void errorMaxCodesExceeded(RequestData requestData) throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(requestData.request, 400, "max-iia-codes exceeded")
    );
  }

  protected void errorMaxIdsExceeded(RequestData requestData) throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(requestData.request, 400, "max-iia-ids exceeded")
    );
  }

  @Override
  protected Response handleIiasGetRequest(Request request) throws ErrorResponseException {
    try {
      RequestData requestData = new RequestData(request);
      checkRequestMethod(requestData.request);
      extractGetParams(requestData);
      checkHei(requestData);
      checkCodes(requestData);
      checkIds(requestData);
      List<Iia> selectedIias = filterIiasByHeiId(iias, requestData);
      List<Iia> selectedIds = filterIiasById(selectedIias, requestData);
      List<Iia> selectedCodes = filterIiasByCode(selectedIias, requestData);
      List<Iia> result = new ArrayList<>();
      result.addAll(selectedIds);
      result.addAll(selectedCodes);
      return createIiasGetResponse(result);
    } catch (ErrorResponseException e) {
      return e.response;
    }
  }

  private List<Iia> filterIiasById(List<Iia> selectedIias, RequestData requestData) {
    List<String> selectedIiaIds = selectedIias.stream()
        .map(i -> i.getPartner().get(0).getIiaId())
        .collect(Collectors.toList());

    List<Iia> result = new ArrayList<>();
    for (String iiaId : requestData.iiaIds) {
      String handledIiaId = null;
      if (selectedIiaIds.contains(iiaId)) {
        handledIiaId = handleKnownIiaId(iiaId);
      } else {
        handledIiaId = handleUnknownIiaId(iiaId, selectedIias);
      }
      if (handledIiaId != null) {
        final String finalHandledIiaId = handledIiaId;
        result.add(selectedIias.stream()
            .filter(i -> i.getPartner().get(0).getIiaId().equals(finalHandledIiaId))
            .findFirst().get());
      }
    }

    return result;
  }

  protected String handleKnownIiaId(String iiaId) {
    return iiaId;
  }

  protected String handleUnknownIiaId(String iiaId, List<Iia> selectedIias) {
    return null;
  }

  protected String handleKnownIiaCode(String iiaCode) {
    return iiaCode;
  }

  protected String handleUnknownIiaCode(String iiaCode, List<Iia> selectedIias) {
    return null;
  }

  protected List<Iia> filterIiasByCode(List<Iia> selectedIias, RequestData requestData) {
    List<String> selectedIiaCodes = selectedIias.stream()
        .map(i -> i.getPartner().get(0).getIiaCode())
        .collect(Collectors.toList());

    List<Iia> result = new ArrayList<>();
    for (String iiaCode : requestData.iiaCodes) {
      String handledIiaCode = null;
      if (selectedIiaCodes.contains(iiaCode)) {
        handledIiaCode = handleKnownIiaCode(iiaCode);
      } else {
        handledIiaCode = handleUnknownIiaCode(iiaCode, selectedIias);
      }
      if (handledIiaCode != null) {
        final String finalHandledIiaCode = handledIiaCode;
        result.add(selectedIias.stream()
            .filter(i -> i.getPartner().get(0).getIiaCode().equals(finalHandledIiaCode))
            .findFirst().get());
      }
    }

    return result;
  }

  private List<String> mapToIds(List<Iia> selectedIias) {
    return selectedIias.stream().map(i -> i.getPartner().get(0).getIiaId())
        .collect(Collectors.toList());
  }

  protected boolean filterPartnerHeiId(Iia.Partner partner, String hei_id) {
    return partner.getHeiId().equals(hei_id);
  }

  protected List<Iia> filterIiasByPartnerHeiId(List<Iia> iias, RequestData requestData) {
    if (requestData.partnerHeiId == null) {
      return iias;
    }

    return iias.stream().filter(
        iia -> iia.getPartner().stream()
            .anyMatch(p -> this.filterPartnerHeiId(p, requestData.partnerHeiId)))
        .collect(Collectors.toList());
  }

  protected List<Iia> filterIiasByHeiId(List<Iia> iias, RequestData requestData) {
    return iias.stream()
        .filter(iia -> iia.getPartner().get(0).getHeiId().equals(requestData.heiId))
        .collect(Collectors.toList());
  }

  protected boolean filterAcademicYear(String academicYear, List<String> requestedAcademicYears) {
    return requestedAcademicYears.contains(academicYear);
  }

  protected List<Iia> filterIiasByAcademicYear(List<Iia> iias, RequestData requestData) {
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
      boolean include = specs.stream().flatMap(m -> m.getReceivingAcademicYearId().stream())
          .anyMatch(ay -> this.filterAcademicYear(ay, requestData.receivingAcademicYearIds));
      if (include) {
        result.add(iia);
      }
    }
    return result;
  }

  private void checkReceivingAcademicYearIds(
      RequestData requestData) throws ErrorResponseException {
    for (String receivingAcademicYear : requestData.receivingAcademicYearIds) {
      if (!checkReceivingAcademicYearId(receivingAcademicYear)) {
        errorInvalidAcademicYearId(requestData);
      }
    }
  }

  private void extractGetParams(RequestData requestData) throws ErrorResponseException {
    checkParamsEncoding(requestData.request);
    Map<String, List<String>> params = InternetTestHelpers.extractAllParams(requestData.request);

    ParameterInfo heiId = ParameterInfo.readParam(params, "hei_id");
    ParameterInfo iiaId = ParameterInfo.readParam(params, "iia_id");
    ParameterInfo iiaCode = ParameterInfo.readParam(params, "iia_code");

    requestData.heiId = heiId.firstValueOrNull;
    requestData.iiaIds = iiaId.allValues;
    requestData.iiaCodes = iiaCode.allValues;

    if (params.size() == 0) {
      errorNoParams(requestData);
    }
    if (!heiId.hasAny) {
      errorNoHeiId(requestData);
    }
    if (heiId.hasMultiple) {
      errorMultipleHeiIds(requestData);
    }
    if (!iiaId.hasAny && !iiaCode.hasAny) {
      errorNoIdsNorCodes(requestData);
    }
    if (iiaId.hasAny && iiaCode.hasAny) {
      errorIdsAndCodes(requestData);
    }

    int expectedParams = 0;
    expectedParams += heiId.coveredParameters;
    expectedParams += iiaId.coveredParameters;
    expectedParams += iiaCode.coveredParameters;
    if (params.size() > expectedParams) {
      handleUnexpectedParams(requestData);
    }

    if (requestData.heiId == null
        || (requestData.iiaIds.isEmpty() && requestData.iiaCodes.isEmpty())) {
      //We expect all of above members to have any value even in invalid scenarios.
      throw new NullPointerException();
    }
  }

  protected void errorNoIdsNorCodes(RequestData requestData) throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(requestData.request, 400, "Neither iia_id nor iia_code provided")
    );
  }

  protected void errorIdsAndCodes(RequestData requestData) throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(requestData.request, 400, "Both iia_id and iia_code provided")
    );
  }

  private void extractIndexParams(RequestData requestData) throws ErrorResponseException {
    checkParamsEncoding(requestData.request);
    Map<String, List<String>> params = InternetTestHelpers.extractAllParams(requestData.request);

    ParameterInfo heiId = ParameterInfo.readParam(params, "hei_id");
    ParameterInfo partnerHeiId = ParameterInfo.readParam(params, "partner_hei_id");
    ParameterInfo receivingAcademicYearId = ParameterInfo
        .readParam(params, "receiving_academic_year_id");
    ParameterInfo modifiedSince = ParameterInfo.readParam(params, "modified_since");

    requestData.heiId = heiId.firstValueOrNull;

    if (params.size() == 0) {
      errorNoParams(requestData);
    }
    if (!heiId.hasAny) {
      errorNoHeiId(requestData);
    }
    if (heiId.hasMultiple) {
      errorMultipleHeiIds(requestData);
    }
    if (partnerHeiId.hasMultiple) {
      errorMultiplePartnerHeiId(requestData);
    }
    if (modifiedSince.hasMultiple) {
      errorMultipleModifiedSince(requestData);
    }

    requestData.partnerHeiId = partnerHeiId.firstValueOrNull;
    requestData.receivingAcademicYearIds = receivingAcademicYearId.allValues;

    if (modifiedSince.firstValueOrNull != null) {
      requestData.modifiedSince = parseModifiedSince(modifiedSince.firstValueOrNull);
      if (requestData.modifiedSince == null) {
        errorInvalidModifiedSince(requestData);
      }
    }

    int expectedParams = 0;
    expectedParams += heiId.coveredParameters;
    expectedParams += partnerHeiId.coveredParameters;
    expectedParams += receivingAcademicYearId.coveredParameters;
    expectedParams += modifiedSince.coveredParameters;
    if (params.size() > expectedParams) {
      handleUnexpectedParams(requestData);
    }

    if (requestData.heiId == null) {
      //We expect all of above members to have any value even in invalid scenarios.
      throw new NullPointerException();
    }
  }

  protected void errorNoParams(RequestData requestData) throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(requestData.request, 400, "No parameters provided")
    );
  }

  protected void errorNoHeiId(RequestData requestData) throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(requestData.request, 400, "No hei_id parameter")
    );
  }

  protected void errorMultipleHeiIds(RequestData requestData) throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(requestData.request, 400, "More that one hei_id provided.")
    );
  }

  protected void errorMultiplePartnerHeiId(RequestData requestData) throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(requestData.request, 400, "More that one partner_hei_id provided.")
    );
  }

  protected void errorMultipleModifiedSince(RequestData requestData) throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(requestData.request, 400, "More that one modified_since provided.")
    );
  }

  protected void handleUnexpectedParams(RequestData requestData) throws ErrorResponseException {
    //Ignore
  }


  static class RequestData {
    public String partnerHeiId;
    public List<String> receivingAcademicYearIds;
    public ZonedDateTime modifiedSince;
    public List<String> iiaIds;
    public List<String> iiaCodes;
    Request request;
    String heiId;

    RequestData(Request request) {
      this.request = request;
    }
  }

  protected void errorInvalidModifiedSince(RequestData requestData) throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(requestData.request, 400, "Invalid modified_since format."));
  }

  private void checkHei(RequestData requestData) throws ErrorResponseException {
    if (!coveredHeiIds.contains(requestData.heiId)) {
      errorUnknownHeiId(requestData);
    } else {
      handleKnownHeiId(requestData);
    }
  }

  protected void handleKnownHeiId(RequestData requestData) {
    //Intentionally left empty
  }

  protected void errorUnknownHeiId(RequestData requestData) throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(requestData.request, 400, "Unknown hei_id")
    );
  }

  protected void checkPartnerHei(RequestData requestData) throws ErrorResponseException {
    if (requestData.heiId.equals(requestData.partnerHeiId)) {
      errorHeiIdsEqual(requestData);
    }

    if (requestData.partnerHeiId != null && !partnersHeiIds.contains(requestData.partnerHeiId)) {
      errorPartnerHeiIdUnknown(requestData);
    }
  }

  protected void errorPartnerHeiIdUnknown(RequestData requestData) throws ErrorResponseException {
    //Ignore
  }

  protected void errorHeiIdsEqual(RequestData requestData) throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(requestData.request, 400, "hei_id and partner_hei_id are equal")
    );
  }

  protected void errorInvalidAcademicYearId(RequestData requestData) throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(requestData.request, 400,
            "receiving_academic_year_id has incorrect format")
    );
  }

  protected Response createIiasGetResponse(List<Iia> data) {
    IiasGetResponse response = new IiasGetResponse();
    response.getIia().addAll(data);
    return marshallResponse(200, response);
  }

  protected Response createIiasIndexResponse(List<String> data) {
    IiasIndexResponse response = new IiasIndexResponse();
    response.getIiaId().addAll(data);
    return marshallResponse(200, response);
  }
}
