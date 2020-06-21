package eu.erasmuswithoutpaper.registry.validators.omobilitylasvalidator;

import java.math.BigInteger;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import eu.erasmuswithoutpaper.registry.internet.InternetTestHelpers;
import eu.erasmuswithoutpaper.registry.internet.Request;
import eu.erasmuswithoutpaper.registry.internet.Response;
import eu.erasmuswithoutpaper.registry.internet.sec.EwpClientWithRsaKey;
import eu.erasmuswithoutpaper.registry.validators.ParameterInfo;
import eu.erasmuswithoutpaper.registry.validators.types.LearningAgreement;
import eu.erasmuswithoutpaper.registry.validators.types.MobilityInstitution;
import eu.erasmuswithoutpaper.registry.validators.types.StringWithOptionalLang;
import eu.erasmuswithoutpaper.registry.validators.types.OmobilityLasUpdateRequest;
import eu.erasmuswithoutpaper.registry.validators.types.ApproveComponentsStudiedProposalV1;
import eu.erasmuswithoutpaper.registry.validators.types.UpdateComponentsStudiedV1;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;


public class OMobilityLAsServiceV030Valid extends AbstractOMobilityLAsService {
  protected List<LearningAgreement> learningAgreements = new ArrayList<>();

  /**
   * @param indexUrl       The endpoint at which to listen for requests.
   * @param getUrl         The endpoint at which to listen for requests.
   * @param updateUrl      The endpoint at which to listen for requests.
   * @param registryClient Initialized and refreshed {@link RegistryClient} instance.
   */
  public OMobilityLAsServiceV030Valid(String indexUrl, String getUrl, String updateUrl,
      RegistryClient registryClient, String heiIdToCover) {
    super(indexUrl, getUrl, updateUrl, registryClient);
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

    MobilityInstitution.ContactPerson contactPerson1 = new MobilityInstitution.ContactPerson();
    contactPerson1.setEmail("test1-contact@example.invalid");
    contactPerson1.setGivenNames("test1-contact");
    contactPerson1.setFamilyName("test1-contact");

    LearningAgreement la1 = new LearningAgreement();
    la1.setOmobilityId("omobility-1");
    la1.setReceivingAcademicYearId("2020/2021");

    MobilityInstitution sendingHei1 = new MobilityInstitution();
    sendingHei1.setHeiId(heiIdToCover);
    sendingHei1.setOunitName("test1-ounit");
    sendingHei1.setOunitId("test1-ounit");
    sendingHei1.setContactPerson(contactPerson1);
    la1.setSendingHei(sendingHei1);

    MobilityInstitution receivingHei1 = new MobilityInstitution();
    receivingHei1.setHeiId(RECEIVING_HEI_ID_1);
    receivingHei1.setOunitName("test1-ounit-recv");
    receivingHei1.setOunitId("test1-ounit-recv");
    receivingHei1.setContactPerson(contactPerson1);
    la1.setReceivingHei(receivingHei1);

    LearningAgreement.Student student1 = new LearningAgreement.Student();
    student1.setGivenNames("test1");
    student1.setFamilyName("test2");
    student1.setBirthDate(xmlDate(1990, 12, 1));
    student1.setCitizenship("PL");
    student1.setGender(BigInteger.valueOf(0));
    student1.setEmail("test1@example.invalid");
    la1.setStudent(student1);

    la1.setPlannedMobilityStart(xmlDate(2020, 8));
    la1.setPlannedMobilityEnd(xmlDate(2021, 2));
    la1.setEqfLevelStudiedAtDeparture((byte) 7);
    la1.setStudentIscedFCode("test1");

    LearningAgreement.StudentLanguageSkill languageSkill1 =
        new LearningAgreement.StudentLanguageSkill();
    languageSkill1.setLanguage("en");
    languageSkill1.setCefrLevel("A2");
    la1.setStudentLanguageSkill(languageSkill1);

    LearningAgreement.ComponentsStudied componentsStudied1 =
        new LearningAgreement.ComponentsStudied();

    LearningAgreement.ComponentsStudied.LatestProposal latestProposal1 =
        new LearningAgreement.ComponentsStudied.LatestProposal();
    latestProposal1.setId("test1");
    componentsStudied1.setLatestProposal(latestProposal1);
    la1.setComponentsStudied(componentsStudied1);

    learningAgreements.add(la1);

    LearningAgreement la2 = new LearningAgreement();
    la2.setOmobilityId("omobility-1");
    la2.setReceivingAcademicYearId("2020/2021");

    MobilityInstitution sendingHei2 = new MobilityInstitution();
    sendingHei2.setHeiId(heiIdToCover);
    sendingHei2.setOunitName("test2-ounit");
    sendingHei2.setOunitId("test2-ounit");
    sendingHei2.setContactPerson(contactPerson1);
    la2.setSendingHei(sendingHei2);

    MobilityInstitution receivingHei2 = new MobilityInstitution();
    receivingHei2.setHeiId(RECEIVING_HEI_ID_2);
    receivingHei2.setOunitName("test2-ounit-recv");
    receivingHei2.setOunitId("test2-ounit-recv");
    receivingHei2.setContactPerson(contactPerson1);
    la2.setReceivingHei(receivingHei2);

    LearningAgreement.Student student2 = new LearningAgreement.Student();
    student2.setGivenNames("test1");
    student2.setFamilyName("test2");
    student2.setBirthDate(xmlDate(1990, 12, 1));
    student2.setCitizenship("PL");
    student2.setGender(BigInteger.valueOf(0));
    student2.setEmail("test1@example.invalid");
    la2.setStudent(student2);

    la2.setPlannedMobilityStart(xmlDate(2020, 8));
    la2.setPlannedMobilityEnd(xmlDate(2021, 2));
    la2.setEqfLevelStudiedAtDeparture((byte) 7);
    la2.setStudentIscedFCode("test1");

    LearningAgreement.StudentLanguageSkill languageSkill2 =
        new LearningAgreement.StudentLanguageSkill();
    languageSkill2.setLanguage("en");
    languageSkill2.setCefrLevel("A2");
    la2.setStudentLanguageSkill(languageSkill2);

    LearningAgreement.ComponentsStudied componentsStudied2 =
        new LearningAgreement.ComponentsStudied();

    LearningAgreement.ComponentsStudied.LatestProposal latestProposal2 =
        new LearningAgreement.ComponentsStudied.LatestProposal();
    latestProposal2.setId("test1");
    componentsStudied2.setLatestProposal(latestProposal2);
    la2.setComponentsStudied(componentsStudied2);

    learningAgreements.add(la2);

//    StudentMobilityForStudiesOMobilitiesV1 mobility2 = new StudentMobilityForStudiesOMobilitiesV1();
//    mobility2.setOmobilityId("omobility-2");
//    mobility2.setReceivingAcademicYearId("2020/2021");
//    StudentMobilityForStudiesOMobilitiesV1.SendingHei sendingHei2 =
//        new StudentMobilityForStudiesOMobilitiesV1.SendingHei();
//    sendingHei2.setHeiId(heiIdToCover);
//    mobility2.setSendingHei(sendingHei2);
//    StudentMobilityForStudiesOMobilitiesV1.ReceivingHei receivingHei2 =
//        new StudentMobilityForStudiesOMobilitiesV1.ReceivingHei();
//    receivingHei2.setHeiId(RECEIVING_HEI_ID_2);
//    mobility2.setReceivingHei(receivingHei2);
//    mobility2.setSendingAcademicTermEwpId("2020/2021-8/9");
//    StudentMobilityForStudiesOMobilitiesV1.Student student2 =
//        new StudentMobilityForStudiesOMobilitiesV1.Student();
//    student2.getGivenNames().add(stringWithOptionalLang("test1"));
//    student2.getFamilyName().add(stringWithOptionalLang("test2"));
//    mobility2.setStudent(student2);
//    mobility2.setStatus(MobilityStatusOMobilitiesV1.LIVE);
//    learningAgreements.add(new LearningAgreement(mobility2, heiIdToCover, RECEIVING_HEI_ID_2));
  }

  private XMLGregorianCalendar xmlDate(int year, int month) {
    return xmlDate(year, month, 1);
  }

  private XMLGregorianCalendar xmlDate(int year, int month, int day) {
    DatatypeFactory datatypeFactory = null;
    try {
      datatypeFactory = DatatypeFactory.newInstance();
    } catch (DatatypeConfigurationException e) {
      return null;
    }

    return datatypeFactory.newXMLGregorianCalendarDate(year, month, day, 0);
  }

  protected int getMaxOmobilityIds() {
    return 3;
  }

  @Override
  protected Response handleOMobilitiesIndexRequest(
      Request request) throws ErrorResponseException {
    try {
      EwpClientWithRsaKey connectedClient = verifyCertificate(request);
      checkRequestMethod(request);

      RequestData requestData = new RequestData(request, connectedClient);
      extractIndexParams(requestData);
      checkSendingHeiId(requestData);
      checkReceivingHeiId(requestData);
      List<LearningAgreement> selectedLAs = filterLearningAgreementsForIndex(learningAgreements, requestData);
      selectedLAs = filterNotPermittedLAs(selectedLAs, requestData);
      selectedLAs = filterLAsByModifiedSince(selectedLAs, requestData);
      selectedLAs = filterLAsByReceivingAcademicYearId(selectedLAs, requestData);
      List<String> resultOmobilityIds = mapToOMobilityIds(selectedLAs);
      return createOMobilityLAsIndexResponse(resultOmobilityIds);
    } catch (ErrorResponseException e) {
      return e.response;
    }
  }

  protected static <T, S> List<T> filterEqual(List<T> data, Function<T, S> valueGetter, S value) {
    if (value == null) {
      return data;
    }
    return data.stream()
        .filter(oMobilityEntry -> value.equals(valueGetter.apply(oMobilityEntry)))
        .collect(Collectors.toList());
  }

  protected static <T> List<T> filter(List<T> data, Predicate<T> predicate) {
    return data.stream()
        .filter(predicate)
        .collect(Collectors.toList());
  }

  protected static <T> List<T> filterModifiedSince(List<T> data, ZonedDateTime modifiedSince) {
    if (modifiedSince == null) {
      return data;
    }
    Instant modifiedAt = Instant.from(
        ZonedDateTime.of(2019, 6, 10, 18, 52, 32, 0, ZoneId.of("Z"))
    );
    if (modifiedSince.toInstant().isAfter(modifiedAt)) {
      return new ArrayList<>();
    } else {
      return data;
    }
  }

  protected static <T, S> List<S> map(List<T> data, Function<T, S> function) {
    return data.stream().map(function).collect(Collectors.toList());
  }


  protected boolean isHeiIdCoveredByClient(RSAPublicKey rsaPublicKey, String receivingHeiId) {
    return this.registryClient.getHeisCoveredByClientKey(rsaPublicKey).contains(receivingHeiId);
  }

  protected List<LearningAgreement> filterLAsByReceivingAcademicYearId(
      List<LearningAgreement> learningAgreements, RequestData requestData) {
    return filterEqual(learningAgreements,
        LearningAgreement::getReceivingAcademicYearId,
        requestData.receivingAcademicYearId
    );
  }

  protected boolean isCalledPermittedToSeeReceivingHeiIdsData(
      RequestData requestData, String receivingHeiId) {
    return isHeiIdCoveredByClient(requestData.client.getRsaPublicKey(), receivingHeiId);
  }

  private List<LearningAgreement> filterNotPermittedLAs(
      List<LearningAgreement> learningAgreements, RequestData requestData) {
    return filter(learningAgreements,
        la -> isCalledPermittedToSeeReceivingHeiIdsData(
            requestData, la.getReceivingHei().getHeiId()
        )
    );
  }

  @Override
  protected Response handleOMobilitiesGetRequest(Request request) throws ErrorResponseException {
    try {
      EwpClientWithRsaKey connectedClient = verifyCertificate(request);
      checkRequestMethod(request);

      RequestData requestData = new RequestData(request, connectedClient);
      extractGetParams(requestData);
      checkSendingHeiId(requestData);
      checkOMobilityIds(requestData);
      List<LearningAgreement> selectedLAs = filterLAsForGet(learningAgreements, requestData);
      selectedLAs = filterNotPermittedLAs(selectedLAs, requestData);
      return createOMobilityLAsGetResponse(selectedLAs);
    } catch (ErrorResponseException e) {
      return e.response;
    }
  }

  @Override
  protected Response handleOMobilitiesUpdateRequest(Request request) throws ErrorResponseException {
    try {
      EwpClientWithRsaKey connectedClient = verifyCertificate(request);
      checkRequestMethod(request, Arrays.asList("POST"));

      RequestData requestData = new RequestData(request, connectedClient);
      extractUpdateParams(requestData);
      checkSendingHeiId(requestData);
      if (requestData.updateRequest.getApproveComponentsStudiedProposalV1() != null) {
        handleApproveComponentsStudiedProposalV1Request(requestData);
      } else if (requestData.updateRequest.getUpdateComponentsStudiedV1() != null) {
        handleUpdateComponentsStudiedV1Request(requestData);
      } else {
        throw new ErrorResponseException(
            createErrorResponse(requestData.request, 400, "Unknown update type.")
        );
      }
      return createOMobilityLAsUpdateResponse("OK");
    } catch (ErrorResponseException e) {
      return e.response;
    }
  }

  private void handleApproveComponentsStudiedProposalV1Request(RequestData requestData)
      throws ErrorResponseException {
    ApproveComponentsStudiedProposalV1 approveComponentsStudiedProposalV1 =
        requestData.updateRequest.getApproveComponentsStudiedProposalV1();
    String omobilityId = approveComponentsStudiedProposalV1.getOmobilityId();
    String requestLatestProposalId = approveComponentsStudiedProposalV1.getLatestProposalId();
    verifyLearningAgreementForUpdate(
        requestData, omobilityId, requestLatestProposalId
    );

    handleApproveComponentsStudiedProposalV1(requestData);
  }

  protected void handleApproveComponentsStudiedProposalV1(RequestData requestData)
      throws ErrorResponseException {
    ApproveComponentsStudiedProposalV1 approveComponentsStudiedProposalV1 =
        requestData.updateRequest.getApproveComponentsStudiedProposalV1();

    if (approveComponentsStudiedProposalV1.getApprovingParty() == null) {
      errorApproveComponentsStudiedProposalV1NoApprovingParty(requestData);
    }
  }

  protected void errorApproveComponentsStudiedProposalV1NoApprovingParty(RequestData requestData)
      throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(requestData.request, 400, "Missing approving party.")
    );
  }

  private void handleUpdateComponentsStudiedV1Request(RequestData requestData)
      throws ErrorResponseException {
    UpdateComponentsStudiedV1 updateComponentsStudiedV1 =
        requestData.updateRequest.getUpdateComponentsStudiedV1();
    String omobilityId = updateComponentsStudiedV1.getOmobilityId();
    String requestLatestProposalId = updateComponentsStudiedV1.getLatestProposalId();
    verifyLearningAgreementForUpdate(requestData, omobilityId, requestLatestProposalId);

    handleUpdateComponentsStudiedV1(requestData);
  }

  protected void handleUpdateComponentsStudiedV1(RequestData requestData)
      throws ErrorResponseException {
    UpdateComponentsStudiedV1 updateComponentsStudiedV1 =
        requestData.updateRequest.getUpdateComponentsStudiedV1();
    if (updateComponentsStudiedV1.getComment() == null && updateComponentsStudiedV1.getSuggestion() == null) {
      throw new ErrorResponseException(
          createErrorResponse(requestData.request, 400,
              "Both suggestions and comments missing.")
      );
    }
  }

  private void verifyLearningAgreementForUpdate(RequestData requestData, String omobilityId,
      String requestLatestProposalId) throws ErrorResponseException {
    if (omobilityId == null) {
      errorNoOMobilityIds(requestData);
    } else {
      requestData.omobilityIds = Arrays.asList(omobilityId);
    }

    if (requestLatestProposalId == null) {
      errorNoLatestProposalId(requestData);
    }

    List<LearningAgreement> updatedAgreement = filter(learningAgreements,
        la -> la.getOmobilityId().equals(omobilityId)
    );

    if (updatedAgreement.isEmpty()) {
      errorUnknownOmobilityIdUpdated(requestData);
    }

    LearningAgreement la = updatedAgreement.get(0);

    if (!isCalledPermittedToSeeReceivingHeiIdsData(requestData, la.getReceivingHei().getHeiId())) {
      errorUpdateCallerNotPermitted(requestData);
    }

    String lasLatestProposalId = la.getComponentsStudied().getLatestProposal().getId();

    if (!lasLatestProposalId.equals(requestLatestProposalId)) {
      errorLatestProposalIdDoNotMatch(requestData);
    }
  }

  private void errorUpdateCallerNotPermitted(RequestData requestData)
      throws ErrorResponseException {
      throw new ErrorResponseException(
          createErrorResponse(requestData.request, 400, "Not permitted.")
      );

  }

  private void errorUnknownOmobilityIdUpdated(RequestData requestData)
      throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(requestData.request, 400, "OMobility id not found")
    );
  }

  private void errorLatestProposalIdDoNotMatch(RequestData requestData)
      throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(requestData.request, 409, "Latest proposal id do not match.")
    );
  }

  private void errorNoLatestProposalId(RequestData requestData) throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(requestData.request, 400, "Missing latest proposal id.")
    );
  }


  private void extractUpdateParams(RequestData requestData) throws ErrorResponseException {
    checkParamsEncoding(requestData.request, "text/xml");
    try {
      requestData.updateRequest = unmarshallObject(requestData.request.getBody().get());
      requestData.sendingHeiId = requestData.updateRequest.getSendingHeiId();
    } catch (JAXBException e) {
      errorInvalidUpdateRequestFormat(requestData);
    }
  }

  private void errorInvalidUpdateRequestFormat(RequestData requestData)
      throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(requestData.request, 400,
            "Invalid request format")
    );
  }

  private void extractIndexParams(RequestData requestData) throws ErrorResponseException {
    checkParamsEncoding(requestData.request);
    Map<String, List<String>> params = InternetTestHelpers.extractAllParams(requestData.request);

    ParameterInfo sendingHeiId = ParameterInfo.readParam(params, "sending_hei_id");
    ParameterInfo receivingHeiId = ParameterInfo.readParam(params, "receiving_hei_id");
    ParameterInfo modifiedSince = ParameterInfo.readParam(params, "modified_since");
    ParameterInfo receivingAcademicYearId =
        ParameterInfo.readParam(params, "receiving_academic_year_id");

    requestData.sendingHeiId = sendingHeiId.firstValueOrNull;
    requestData.receivingHeiIds = receivingHeiId.allValues;
    requestData.modifiedSinceString = modifiedSince.firstValueOrNull;
    requestData.receivingAcademicYearId = receivingAcademicYearId.firstValueOrNull;

    if (params.size() == 0) {
      errorNoParams(requestData);
    }
    if (!sendingHeiId.hasAny) {
      errorNoSendingHeiId(requestData);
    }
    if (sendingHeiId.hasMultiple) {
      errorMultipleSendingHeiIds(requestData);
    }
    if (!receivingHeiId.hasAny) {
      handleNoReceivingHeiId(requestData);
    }
    if (receivingHeiId.hasMultiple) {
      handleMultipleReceivingHeiId(requestData);
    }
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
    expectedParams += sendingHeiId.coveredParameters;
    expectedParams += receivingHeiId.coveredParameters;
    expectedParams += modifiedSince.coveredParameters;
    expectedParams += receivingAcademicYearId.coveredParameters;
    if (params.size() > expectedParams) {
      handleUnexpectedParams(requestData);
    }

    if (requestData.sendingHeiId == null || requestData.receivingHeiIds == null) {
      //We expect all of above members to have any value even in invalid scenarios.
      throw new NullPointerException();
    }
  }

  private void extractGetParams(RequestData requestData) throws ErrorResponseException {
    checkParamsEncoding(requestData.request);
    Map<String, List<String>> params = InternetTestHelpers.extractAllParams(requestData.request);

    ParameterInfo sendingHeiId = ParameterInfo.readParam(params, "sending_hei_id");
    ParameterInfo omobilityIds = ParameterInfo.readParam(params, "omobility_id");

    requestData.sendingHeiId = sendingHeiId.firstValueOrNull;
    requestData.omobilityIds = omobilityIds.allValues;

    if (params.size() == 0) {
      errorNoParams(requestData);
    }
    if (!sendingHeiId.hasAny) {
      errorNoSendingHeiId(requestData);
    }
    if (sendingHeiId.hasMultiple) {
      errorMultipleSendingHeiIds(requestData);
    }
    if (!omobilityIds.hasAny) {
      errorNoOMobilityIds(requestData);
    }

    int expectedParams = 0;
    expectedParams += sendingHeiId.coveredParameters;
    expectedParams += omobilityIds.coveredParameters;
    if (params.size() > expectedParams) {
      handleUnexpectedParams(requestData);
    }

    if (requestData.sendingHeiId == null || requestData.omobilityIds.isEmpty()) {
      //We expect all of above members to have any value even in invalid scenarios.
      throw new NullPointerException();
    }
  }

  protected void checkReceivingHeiId(RequestData requestData) throws ErrorResponseException {
    List<String> knownReceivingHeiIds = map(learningAgreements,
        la -> la.getReceivingHei().getHeiId());
    if (!knownReceivingHeiIds.containsAll(requestData.receivingHeiIds)) {
      handleUnknownReceivingHeiId(requestData);
    }
  }

  protected void checkSendingHeiId(RequestData requestData) throws ErrorResponseException {
    if (learningAgreements.stream().noneMatch(
        la -> la.getSendingHei().getHeiId().equals(requestData.sendingHeiId)
    )) {
      handleUnknownSendingHeiId(requestData);
    }
  }

  protected void checkOMobilityIds(RequestData requestData) throws ErrorResponseException {
    if (requestData.omobilityIds.size() > getMaxOmobilityIds()) {
      errorMaxOMobilityIdsExceeded(requestData);
    }

    List<String> knownOMobilityIds = map(learningAgreements, LearningAgreement::getOmobilityId);

    if (!knownOMobilityIds.containsAll(requestData.omobilityIds)) {
      handleUnknownOMobilityId(requestData);
    }
  }

  protected List<LearningAgreement> filterLAsByModifiedSince(
      List<LearningAgreement> learningAgreements, RequestData requestData) {
    return filterModifiedSince(learningAgreements, requestData.modifiedSince);
  }

  protected boolean isLearningAgreementSelectedByRequest(LearningAgreement la,
      RequestData requestData) {
    boolean sendingMatches = requestData.sendingHeiId.equals(la.getSendingHei().getHeiId());

    boolean receivingMatches = false;
    if (requestData.receivingHeiIds.isEmpty()) {
      receivingMatches = true;
    } else if (requestData.receivingHeiIds.contains(la.getReceivingHei().getHeiId())) {
      receivingMatches = true;
    }

    return sendingMatches && receivingMatches;
  }

  protected List<LearningAgreement> filterLearningAgreementsForIndex(
      List<LearningAgreement> learningAgreements, RequestData requestData) {
    return filter(learningAgreements, la -> isLearningAgreementSelectedByRequest(la, requestData));
  }

  protected boolean filterLAsForGet(LearningAgreement la, RequestData requestData) {
    return requestData.sendingHeiId.equals(la.getSendingHei().getHeiId())
        && requestData.omobilityIds.contains(la.getOmobilityId());
  }

  protected List<LearningAgreement> filterLAsForGet(
      List<LearningAgreement> learningAgreements, RequestData requestData) {
    return filter(learningAgreements, la -> filterLAsForGet(la, requestData));
  }

  private List<String> mapToOMobilityIds(List<LearningAgreement> learningAgreements) {
    return map(learningAgreements, LearningAgreement::getOmobilityId);
  }

  protected void errorMaxOMobilityIdsExceeded(
      RequestData requestData) throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(requestData.request, 400, "max-omobility-ids exceeded")
    );
  }

  protected void errorNoOMobilityIds(RequestData requestData) throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(requestData.request, 400, "No omobility_id provided")
    );
  }

  protected void errorNoParams(RequestData requestData) throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(requestData.request, 400, "No parameters provided")
    );
  }

  protected void errorNoSendingHeiId(RequestData requestData) throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(requestData.request, 400, "No sending_hei_id parameter")
    );
  }

  protected void errorMultipleSendingHeiIds(
      RequestData requestData) throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(requestData.request, 400, "More that one sending_hei_id provided.")
    );
  }

  protected void handleMultipleReceivingHeiId(RequestData requestData) throws ErrorResponseException {
    // ignore
  }

  protected void errorMultipleModifiedSince(RequestData requestData) throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(requestData.request, 400, "More than one modified_since provided.")
    );
  }

  protected void errorInvalidModifiedSince(RequestData requestData) throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(requestData.request, 400, "Invalid modified_since format."));
  }

  protected void errorMultipleReceivingAcademicYearIds(RequestData requestData)
      throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(requestData.request, 400, "More than one receiving_academic_year_id provided.")
    );
  }

  protected void errorInvalidReceivingAcademicYearId(RequestData requestData)
      throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(requestData.request, 400, "Invalid receiving_academic_year_id format."));
  }

  protected void handleUnexpectedParams(RequestData requestData) throws ErrorResponseException {
    //Ignore
  }

  protected void handleUnknownOMobilityId(RequestData requestData) throws ErrorResponseException {
    //Ignore
  }

  protected void handleUnknownSendingHeiId(
      RequestData requestData) throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(requestData.request, 400, "Unknown sending_hei_id")
    );
  }

  protected void handleUnknownReceivingHeiId(RequestData requestData) throws ErrorResponseException {
    //Ignore
  }

  protected void handleNoReceivingHeiId(RequestData requestData) throws ErrorResponseException {
    // ignore
  }


  static class RequestData {
    public String sendingHeiId;
    public List<String> receivingHeiIds;
    public ZonedDateTime modifiedSince;
    public String modifiedSinceString;
    public List<String> omobilityIds;
    public String receivingAcademicYearId;
    public OmobilityLasUpdateRequest updateRequest;
    Request request;
    EwpClientWithRsaKey client;

    RequestData(Request request, EwpClientWithRsaKey client) {
      this.request = request;
      this.client = client;
    }

  }
}
