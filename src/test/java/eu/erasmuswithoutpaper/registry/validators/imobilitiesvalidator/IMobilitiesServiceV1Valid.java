package eu.erasmuswithoutpaper.registry.validators.imobilitiesvalidator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

import eu.erasmuswithoutpaper.registry.internet.InternetTestHelpers;
import eu.erasmuswithoutpaper.registry.internet.Request;
import eu.erasmuswithoutpaper.registry.internet.Response;
import eu.erasmuswithoutpaper.registry.internet.sec.EwpClientWithRsaKey;
import eu.erasmuswithoutpaper.registry.validators.ParameterInfo;
import eu.erasmuswithoutpaper.registry.validators.ValidatorKeyStore;
import eu.erasmuswithoutpaper.registry.validators.types.ImobilitiesGetResponse;
import eu.erasmuswithoutpaper.registry.validators.types.StudentMobilityForStudies;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;
import eu.erasmuswithoutpaper.registry.validators.types.NominationStatus;


public class IMobilitiesServiceV1Valid extends AbstractIMobilitiesService {
  final String validatorHeiId = "validator-hei01.developers.erasmuswithoutpaper.eu";
  // hei_id -> omobility_id -> mobility
  protected Map<String, Map<String, MobilityInfo>> mobilitiesCoveredByHeiIds =
      new HashMap<>();

  public IMobilitiesServiceV1Valid(String url, RegistryClient registryClient,
      ValidatorKeyStore validatorKeyStore) {
    super(url, registryClient);
    try {
      for (String heiId : validatorKeyStore.getCoveredHeiIDs()) {
        Map<String, MobilityInfo> studentMobilityInfos = new HashMap<>();
          studentMobilityInfos.put("sm1", createStudentMobilitiesInfo("sm1", validatorHeiId));
        studentMobilityInfos.put("sm2", createStudentMobilitiesInfo("sm2", validatorHeiId));
        studentMobilityInfos.put("sm3", createStudentMobilitiesInfo("sm3", "hei-1"));
        mobilitiesCoveredByHeiIds.put(heiId, studentMobilityInfos);
      }
    } catch (DatatypeConfigurationException ignore) {
    }
  }

  protected int getMaxOMobilityIds() {
    return 3;
  }

  private MobilityInfo createStudentMobilitiesInfo(String omobilityId, String coveringHeiId)
      throws DatatypeConfigurationException {
    StudentMobilityForStudies mobility = new StudentMobilityForStudies();
    mobility.setStatus(NominationStatus.VERIFIED);
    mobility.setActualArrivalDate(
        DatatypeFactory.newInstance().newXMLGregorianCalendar(2018, 2, 14, 12, 22, 10, 0, 120)
    );
    mobility.setActualDepartureDate(
        DatatypeFactory.newInstance().newXMLGregorianCalendar(2018, 5, 14, 12, 22, 10, 0, 120)
    );
    mobility.setOmobilityId(omobilityId);
    return new MobilityInfo(
        coveringHeiId,
        mobility
    );
  }

  protected Response createIMobilitiesGetResponse(List<StudentMobilityForStudies> mobilities) {
    ImobilitiesGetResponse response = new ImobilitiesGetResponse();
    response.getSingleIncomingMobilityObject().addAll(mobilities);
    return marshallResponse(200, response);
  }

  @Override
  public Response handleIMobilitiesGetInternetRequest(Request request) {
    try {
      EwpClientWithRsaKey connectedClient = verifyCertificate(request);
      checkRequestMethod(request);
      RequestData requestData = new RequestData(request, connectedClient);
      extractParams(requestData);
      checkReceivingHeiId(requestData);
      checkOmobilities(requestData);
      List<StudentMobilityForStudies> mobilitiesData = processOMobilities(requestData);
      return createIMobilitiesGetResponse(mobilitiesData);
    } catch (ErrorResponseException e) {
      return e.response;
    }
  }

  private void checkReceivingHeiId(RequestData requestData) throws ErrorResponseException {
    if (!mobilitiesCoveredByHeiIds.containsKey(requestData.receivingHeiId)) {
      handleUnknownReceivingHeiId(requestData);
    }
  }

  protected void handleUnknownReceivingHeiId(RequestData requestData) throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(requestData.request, 400, "Unknown receiving-hei-id"));
  }

  protected void checkOmobilities(RequestData requestData) throws ErrorResponseException {
    if (requestData.omobilities.size() > getMaxOMobilityIds()) {
      errorMaxOmobilityIdsExceeded(requestData);
    }
  }

  private void extractParams(RequestData requestData) throws ErrorResponseException {
    checkParamsEncoding(requestData.request);
    Map<String, List<String>> params = InternetTestHelpers.extractAllParams(requestData.request);
    ParameterInfo receivingHeiId = ParameterInfo.readParam(params, "receiving_hei_id");

    if (!receivingHeiId.hasAny) {
      handleNoReceivingHeiIdParam(requestData);
    }
    if (receivingHeiId.hasMultiple) {
      handleMultipleReceivingHeiIdParams(requestData);
    }
    requestData.receivingHeiId = receivingHeiId.getValueOrDefault(
        mobilitiesCoveredByHeiIds.keySet().iterator().next()
    );

    ParameterInfo omobilityId = ParameterInfo.readParam(params, "omobility_id");
    if (!omobilityId.hasAny) {
      handleNoOMobilityId(requestData);
    }
    requestData.omobilities = omobilityId.allValues;

    if (params.size() > 2) {
      handleUnknownParams(requestData);
    }
  }

  protected void handleUnknownParams(RequestData requestData)
      throws ErrorResponseException {
    // ignore
  }

  protected void handleMultipleReceivingHeiIdParams(RequestData requestData)
      throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(requestData.request, 400, "Expected a single \"receiving_hei_id\" parameters"));
  }

  protected void handleNoReceivingHeiIdParam(RequestData requestData)
      throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(requestData.request, 400, "Expected \"receiving_hei_id\" parameters"));
  }

  protected void handleNoOMobilityId(RequestData requestData) throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(requestData.request, 400, "Expected \"omobility_id\" parameters"));
  }

  protected StudentMobilityForStudies processCoveredOMobilityId(RequestData requestData,
      String receivingHeiId, String omobilityId)
      throws ErrorResponseException {
    return mobilitiesCoveredByHeiIds.get(receivingHeiId).get(omobilityId).mobility;
  }

  protected StudentMobilityForStudies processNotCoveredOMobilityId(RequestData requestData,
      String receivingHeiId, String omobilityId)
      throws ErrorResponseException {
    //Ignore
    return null;
  }

  protected List<StudentMobilityForStudies> processOMobilities(RequestData requestData)
      throws ErrorResponseException {

    List<StudentMobilityForStudies> result = new ArrayList<>();
    for (String omobilityId : requestData.omobilities) {
      StudentMobilityForStudies mobility;
      if (mobilitiesCoveredByHeiIds.get(requestData.receivingHeiId).containsKey(omobilityId) &&
          isCalledPermittedToSeeHeiData(omobilityId, requestData)) {
        mobility =
            processCoveredOMobilityId(requestData, requestData.receivingHeiId, omobilityId);
      } else {
        mobility =
            processNotCoveredOMobilityId(requestData, requestData.receivingHeiId, omobilityId);
      }

      if (mobility != null) {
        result.add(mobility);
      }
    }
    return result;
  }

  protected void errorMaxOmobilityIdsExceeded(RequestData requestData)
      throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(requestData.request, 400, "Exceeded max-omobility-ids"));
  }

  protected boolean isCalledPermittedToSeeHeiData(String omobilityId, RequestData requestData) {
    String heiId =
        mobilitiesCoveredByHeiIds.get(requestData.receivingHeiId).get(omobilityId).coveringHeiId;
    return this.registryClient.isHeiCoveredByClientKey(heiId, requestData.client.getRsaPublicKey());
  }


  protected static class MobilityInfo {
    StudentMobilityForStudies mobility;
    String coveringHeiId;

    MobilityInfo(String coveringHeiId, StudentMobilityForStudies mobility) {
      this.coveringHeiId = coveringHeiId;
      this.mobility = mobility;
    }
  }


  static class RequestData {
    EwpClientWithRsaKey client;
    Request request;
    String receivingHeiId;
    List<String> omobilities;

    RequestData(Request request, EwpClientWithRsaKey client) {
      this.request = request;
      this.client = client;
    }
  }
}
