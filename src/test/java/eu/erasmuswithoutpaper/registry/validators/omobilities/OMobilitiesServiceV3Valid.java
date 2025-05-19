package eu.erasmuswithoutpaper.registry.validators.omobilities;

import java.math.BigInteger;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import eu.erasmuswithoutpaper.registry.internet.InternetTestHelpers;
import eu.erasmuswithoutpaper.registry.internet.Request;
import eu.erasmuswithoutpaper.registry.internet.Response;
import eu.erasmuswithoutpaper.registry.internet.sec.EwpClientWithRsaKey;
import eu.erasmuswithoutpaper.registry.validators.ParameterInfo;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

import https.github_com.erasmus_without_paper.ewp_specs_api_omobilities.blob.stable_v3.endpoints.get_response.MobilityActivityAttributes;
import https.github_com.erasmus_without_paper.ewp_specs_api_omobilities.blob.stable_v3.endpoints.get_response.MobilityActivityType;
import https.github_com.erasmus_without_paper.ewp_specs_api_omobilities.blob.stable_v3.endpoints.get_response.MobilityStatus;
import https.github_com.erasmus_without_paper.ewp_specs_api_omobilities.blob.stable_v3.endpoints.get_response.NominationType;
import https.github_com.erasmus_without_paper.ewp_specs_api_omobilities.blob.stable_v3.endpoints.get_response.OmobilitiesGetResponse;
import https.github_com.erasmus_without_paper.ewp_specs_api_omobilities.blob.stable_v3.endpoints.get_response.StudentMobility;
import https.github_com.erasmus_without_paper.ewp_specs_api_omobilities.blob.stable_v3.endpoints.index_response.OmobilitiesIndexResponse;
import https.github_com.erasmus_without_paper.ewp_specs_architecture.blob.stable_v1.common_types.StringWithOptionalLang;


public class OMobilitiesServiceV3Valid extends AbstractOMobilitiesService {
  protected List<OMobilityEntry> mobilities = new ArrayList<>();

  /**
   * @param indexUrl The endpoint at which to listen for requests.
   * @param getUrl The endpoint at which to listen for requests.
   * @param registryClient Initialized and refreshed {@link RegistryClient} instance.
   */
  public OMobilitiesServiceV3Valid(String indexUrl, String getUrl, RegistryClient registryClient,
      String heiIdToCover) {
    super(indexUrl, getUrl, registryClient);
    fillDataBase(heiIdToCover);
  }

  private StringWithOptionalLang stringWithOptionalLang(String string) {
    StringWithOptionalLang result = new StringWithOptionalLang();
    result.setValue(string);
    return result;
  }

  private void fillDataBase(String heiIdToCover) {
    final String RECEIVING_HEI_ID_1 = "validator-hei01.developers.erasmuswithoutpaper.eu";
    final String RECEIVING_HEI_ID_2 = "uw.edu.pl";

    StudentMobility mobility1 = new StudentMobility();
    mobility1.setOmobilityId("omobility-1");
    mobility1.setStudent(getStudent("1"));
    mobility1.setNomination(getNomination(heiIdToCover, RECEIVING_HEI_ID_1));
    mobility1.setStatus(MobilityStatus.APPROVED);
    mobilities.add(new OMobilityEntry(mobility1, heiIdToCover, RECEIVING_HEI_ID_1));


    StudentMobility mobility2 = new StudentMobility();
    mobility2.setOmobilityId("omobility-2");
    mobility2.setStudent(getStudent("2"));
    mobility2.setNomination(getNomination(heiIdToCover, RECEIVING_HEI_ID_2));
    mobility2.setStatus(MobilityStatus.APPROVED);
    mobilities.add(new OMobilityEntry(mobility2, heiIdToCover, RECEIVING_HEI_ID_2));
  }

  private StudentMobility.Student getStudent(String globalId) {
    StudentMobility.Student student1 = new StudentMobility.Student();
    student1.getGivenNames().add(stringWithOptionalLang("test1"));
    student1.getFamilyName().add(stringWithOptionalLang("test2"));
    student1.setGlobalId(globalId);
    student1.setBirthDate(xmlDate(1990, 12, 1));
    student1.setNationality("NO");
    student1.setGender(BigInteger.valueOf(1));
    student1.setEmail("email@example.com");
    return student1;
  }

  private static NominationType getNomination(String heiIdToCover, String receivingHeiId) {
    NominationType nomination = new NominationType();
    NominationType.SendingHei sendingHei1 = new NominationType.SendingHei();
    sendingHei1.setHeiId(heiIdToCover);
    nomination.setSendingHei(sendingHei1);
    NominationType.ReceivingHei receivingHei1 = new NominationType.ReceivingHei();
    receivingHei1.setHeiId(receivingHeiId);
    nomination.setReceivingHei(receivingHei1);
    nomination.setReceivingAcademicYearId("2020/2021");
    nomination.setSendingAcademicTermEwpId("2020/2021-1/2");
    nomination.setActivityType(MobilityActivityType.STUDENT_STUDIES);
    nomination.setActivityAttributes(MobilityActivityAttributes.LONG_TERM);
    nomination.setAgreementIscedFCode("1234");
    nomination.setEqfLevelStudiedAtDeparture((byte) 7);
    nomination.setNomineeIscedFCode("1234");
    nomination.setProposalId("AE61266750D019063512516C7EE01968012C81F25A89");
    return nomination;
  }

  protected int getMaxOmobilityIds() {
    return 3;
  }

  @Override
  protected Response handleOMobilitiesIndexRequest(Request request) {
    try {
      EwpClientWithRsaKey connectedClient = verifyCertificate(request);
      checkRequestMethod(request);

      RequestData requestData = new RequestData(request, connectedClient);
      extractIndexParams(requestData);
      List<OMobilityEntry> selectedOMobilities = filterNotPermittedOMobilities(mobilities, requestData);
      selectedOMobilities = filterOMobilitiesByModifiedSince(selectedOMobilities, requestData);
      selectedOMobilities =
          filterOMobilitiesByReceivingAcademicYearId(selectedOMobilities, requestData);
      List<String> resultOMobilities = mapToOMobilityIds(selectedOMobilities);
      return createOMobilitiesIndexResponse(resultOMobilities);
    } catch (ErrorResponseException e) {
      return e.response;
    }
  }

  protected List<OMobilityEntry> filterOMobilitiesByReceivingAcademicYearId(
      List<OMobilityEntry> selectedOMobilities, RequestData requestData) {
    if (requestData.receivingAcademicYearId == null) {
      return selectedOMobilities;
    }
    return selectedOMobilities.stream()
        .filter(oMobilityEntry -> requestData.receivingAcademicYearId
            .equals(oMobilityEntry.mobility.getNomination().getReceivingAcademicYearId()))
        .collect(Collectors.toList());
  }

  protected boolean isCallerCoveringHeiId(RequestData requestData, String heiId) {
    return this.registryClient.getHeisCoveredByClientKey(requestData.client.getRsaPublicKey())
        .contains(heiId);
  }

  private List<OMobilityEntry> filterNotPermittedOMobilities(
      List<OMobilityEntry> selectedOMobilities, RequestData requestData) {
    return selectedOMobilities.stream()
        .filter(mobility -> isCallerCoveringHeiId(requestData, mobility.receiving_hei_id))
        .collect(Collectors.toList());
  }

  @Override
  protected Response handleOMobilitiesGetRequest(Request request) {
    try {
      EwpClientWithRsaKey connectedClient = verifyCertificate(request);
      checkRequestMethod(request);

      RequestData requestData = new RequestData(request, connectedClient);
      extractGetParams(requestData);
      checkOMobilityIds(requestData);
      List<OMobilityEntry> selectedOMobilities = filterOMobilitiesForGet(mobilities, requestData);
      List<StudentMobility> result = selectedOMobilities.stream()
          .map(oMobilityEntry -> oMobilityEntry.mobility).collect(Collectors.toList());
      return createOMobilitiesGetResponse(result);
    } catch (ErrorResponseException e) {
      return e.response;
    }
  }

  private void extractIndexParams(RequestData requestData) throws ErrorResponseException {
    checkParamsEncoding(requestData.request);
    Map<String, List<String>> params = InternetTestHelpers.extractAllParams(requestData.request);

    ParameterInfo modifiedSince = ParameterInfo.readParam(params, "modified_since");
    ParameterInfo receivingAcademicYearId =
        ParameterInfo.readParam(params, "receiving_academic_year_id");

    requestData.modifiedSinceString = modifiedSince.firstValueOrNull;
    requestData.receivingAcademicYearId = receivingAcademicYearId.firstValueOrNull;

    if (modifiedSince.hasMultiple) {
      errorMultipleModifiedSince(requestData);
    }
    if (receivingAcademicYearId.hasMultiple) {
      errorMultipleReceivingAcademicYearIds(requestData);
    }

    if (requestData.modifiedSinceString != null && requestData.modifiedSince == null) {
      requestData.modifiedSince = parseModifiedSince(requestData.modifiedSinceString);
      if (requestData.modifiedSince == null) {
        errorInvalidModifiedSince(requestData);
      }
    }

    if (requestData.receivingAcademicYearId != null) {
      if (!checkReceivingAcademicYearId(requestData.receivingAcademicYearId)) {
        errorInvalidReceivingAcademicYearId(requestData);
      }
    }

    int expectedParams = 0;
    expectedParams += modifiedSince.coveredParameters;
    expectedParams += receivingAcademicYearId.coveredParameters;
    if (params.size() > expectedParams) {
      handleUnexpectedParams(requestData);
    }
  }

  private void extractGetParams(RequestData requestData) throws ErrorResponseException {
    checkParamsEncoding(requestData.request);
    Map<String, List<String>> params = InternetTestHelpers.extractAllParams(requestData.request);

    ParameterInfo omobilityIds = ParameterInfo.readParam(params, "omobility_id");

    requestData.omobilityIds = omobilityIds.allValues;

    if (params.isEmpty()) {
      errorNoParams(requestData);
    }
    if (!omobilityIds.hasAny) {
      errorNoOMobilityIds(requestData);
    }

    int expectedParams = 0;
    expectedParams += omobilityIds.coveredParameters;
    if (params.size() > expectedParams) {
      handleUnexpectedParams(requestData);
    }
  }

  protected void checkOMobilityIds(RequestData requestData) throws ErrorResponseException {
    if (requestData.omobilityIds.size() > getMaxOmobilityIds()) {
      errorMaxOMobilityIdsExceeded(requestData);
    }

    List<String> knownOMobilityIds = mobilities.stream()
        .map(mobility -> mobility.mobility.getOmobilityId()).toList();
    if (!knownOMobilityIds.containsAll(requestData.omobilityIds)) {
      handleUnknownOMobilityId(requestData);
    }
  }

  protected List<OMobilityEntry> filterOMobilitiesByModifiedSince(
      List<OMobilityEntry> selectedOMobilities, RequestData requestData) {
    if (requestData.modifiedSince == null) {
      return selectedOMobilities;
    }
    Instant modifiedAt = Instant.from(ZonedDateTime.of(2019, 6, 10, 18, 52, 32, 0, ZoneId.of("Z")));
    if (requestData.modifiedSince.toInstant().isAfter(modifiedAt)) {
      return new ArrayList<>();
    } else {
      return selectedOMobilities;
    }
  }

  protected boolean filterOMobilitiesForGet(OMobilityEntry mobility, RequestData requestData) {
    return requestData.omobilityIds.contains(mobility.mobility.getOmobilityId());
  }

  protected List<OMobilityEntry> filterOMobilitiesForGet(List<OMobilityEntry> tors,
      RequestData requestData) {
    return tors.stream().filter(mobility -> filterOMobilitiesForGet(mobility, requestData))
        .collect(Collectors.toList());
  }

  private List<String> mapToOMobilityIds(List<OMobilityEntry> selectedOMobilities) {
    return selectedOMobilities.stream().map(mobility -> mobility.mobility.getOmobilityId())
        .collect(Collectors.toList());
  }

  protected void errorMaxOMobilityIdsExceeded(RequestData requestData)
      throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(requestData.request, 400, "max-omobility-ids exceeded"));
  }

  protected void errorNoOMobilityIds(RequestData requestData) throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(requestData.request, 400, "No omobility_id provided"));
  }

  protected void errorNoParams(RequestData requestData) throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(requestData.request, 400, "No parameters provided"));
  }

  protected void errorMultipleModifiedSince(RequestData requestData) throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(requestData.request, 400, "More than one modified_since provided."));
  }

  protected void errorInvalidModifiedSince(RequestData requestData) throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(requestData.request, 400, "Invalid modified_since format."));
  }

  protected void errorMultipleReceivingAcademicYearIds(RequestData requestData)
      throws ErrorResponseException {
    throw new ErrorResponseException(createErrorResponse(requestData.request, 400,
        "More than one receiving_academic_year_id provided."));
  }

  protected void errorInvalidReceivingAcademicYearId(RequestData requestData)
      throws ErrorResponseException {
    throw new ErrorResponseException(createErrorResponse(requestData.request, 400,
        "Invalid receiving_academic_year_id format."));
  }

  protected void handleUnexpectedParams(RequestData requestData) throws ErrorResponseException {
    // Ignore
  }

  protected void handleUnknownOMobilityId(RequestData requestData) throws ErrorResponseException {
    // Ignore
  }

  protected Response createOMobilitiesGetResponse(List<StudentMobility> data) {
    OmobilitiesGetResponse response = new OmobilitiesGetResponse();
    response.getSingleMobilityObject().addAll(data);
    return marshallResponse(200, response);
  }

  protected Response createOMobilitiesIndexResponse(List<String> data) {
    OmobilitiesIndexResponse response = new OmobilitiesIndexResponse();
    response.getOmobilityId().addAll(data);
    return marshallResponse(200, response);
  }

  protected static class OMobilityEntry {
    public StudentMobility mobility;
    public String sending_hei_id;
    public String receiving_hei_id;

    public OMobilityEntry(StudentMobility mobility, String sending_hei_id,
        String receiving_hei_id) {
      this.mobility = mobility;
      this.sending_hei_id = sending_hei_id;
      this.receiving_hei_id = receiving_hei_id;
    }
  }

  protected static class RequestData {
    public ZonedDateTime modifiedSince;
    public String modifiedSinceString;
    public List<String> omobilityIds;
    public String receivingAcademicYearId;
    Request request;
    EwpClientWithRsaKey client;

    RequestData(Request request, EwpClientWithRsaKey client) {
      this.request = request;
      this.client = client;
    }
  }
}
