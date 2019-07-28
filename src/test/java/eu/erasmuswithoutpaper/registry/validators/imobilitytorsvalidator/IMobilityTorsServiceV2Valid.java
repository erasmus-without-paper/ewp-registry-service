package eu.erasmuswithoutpaper.registry.validators.imobilitytorsvalidator;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import eu.erasmuswithoutpaper.registry.internet.InternetTestHelpers;
import eu.erasmuswithoutpaper.registry.internet.Request;
import eu.erasmuswithoutpaper.registry.internet.Response;
import eu.erasmuswithoutpaper.registry.validators.types.ImobilityTorsGetResponse;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;
import org.springframework.core.io.ResourceLoader;

public class IMobilityTorsServiceV2Valid extends AbstractIMobilityTorsService {
  protected class IMobilityTorEntry {
    public ImobilityTorsGetResponse.Tor tor;
    public String receiving_hei_id;
    public String sending_hei_id;

    public IMobilityTorEntry(
        ImobilityTorsGetResponse.Tor tor, String receiving_hei_id, String sending_hei_id) {
      this.tor = tor;
      this.receiving_hei_id = receiving_hei_id;
      this.sending_hei_id = sending_hei_id;
    }
  }

  protected List<IMobilityTorEntry> tors = new ArrayList<>();


  /**
   * @param indexUrl
   *     The endpoint at which to listen for requests.
   * @param getUrl
   *     The endpoint at which to listen for requests.
   * @param registryClient
   *     Initialized and refreshed {@link RegistryClient} instance.
   */
  public IMobilityTorsServiceV2Valid(String indexUrl, String getUrl,
      RegistryClient registryClient, ResourceLoader resourceLoader) {
    super(indexUrl, getUrl, registryClient);
    fillDataBase(resourceLoader);
  }

  private List<ImobilityTorsGetResponse.Tor> readTorFromFile(ResourceLoader resourceLoader) {
    String filename = "imobilitytorsvalidator/tor.xml";
    try {
      JAXBContext jc = JAXBContext.newInstance(ImobilityTorsGetResponse.class);
      ImobilityTorsGetResponse parsedRespone = (ImobilityTorsGetResponse) jc.createUnmarshaller().unmarshal(
          resourceLoader.getResource("classpath:test-files/" + filename).getInputStream());
      return parsedRespone.getTor();
    } catch (JAXBException | IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void fillDataBase(ResourceLoader resourceLoader) {
    final String RECEIVING_HEI_ID = "test.hei01.uw.edu.pl";
    final String SENDING_HEI_ID_1 = "validator-hei01.developers.erasmuswithoutpaper.eu";
    final String SENDING_HEI_ID_2 = "test.eu";

    List<ImobilityTorsGetResponse.Tor> tor = readTorFromFile(resourceLoader);
    tors.add(new IMobilityTorEntry(tor.get(0), RECEIVING_HEI_ID, SENDING_HEI_ID_1));
    tors.add(new IMobilityTorEntry(tor.get(1), RECEIVING_HEI_ID, SENDING_HEI_ID_2));
  }

  protected int getMaxOmobilityIds() {
    return 3;
  }

  @Override
  protected Response handleIMobilityTorsIndexRequest(
      Request request) throws ErrorResponseException {
    try {
      RequestData requestData = new RequestData(request);
      checkRequestMethod(requestData.request);
      extractIndexParams(requestData);
      checkReceivingHeiId(requestData);
      checkSendingHeiId(requestData);
      List<IMobilityTorEntry> selectedIMobilityTors = filterIMobilityTorsForIndex(tors,
          requestData);
      selectedIMobilityTors = filterIMobilityTorsByModifiedSince(selectedIMobilityTors,
          requestData);
      List<String> resultOMobilities = mapToOMobilityIds(selectedIMobilityTors);
      return createIMobilityTorsIndexResponse(resultOMobilities);
    } catch (ErrorResponseException e) {
      return e.response;
    }
  }

  @Override
  protected Response handleIMobilityTorsGetRequest(Request request) throws ErrorResponseException {
    try {
      RequestData requestData = new RequestData(request);
      checkRequestMethod(requestData.request);
      extractGetParams(requestData);
      checkReceivingHeiId(requestData);
      checkOMobilityIds(requestData);
      List<IMobilityTorEntry> selectedIMobilityTors = filterIMobilityTorsForGet(tors, requestData);
      List<ImobilityTorsGetResponse.Tor> result = selectedIMobilityTors.stream().map(tor -> tor.tor)
          .collect(Collectors.toList());
      return createIMobilityTorsGetResponse(result);
    } catch (ErrorResponseException e) {
      return e.response;
    }
  }

  private void extractIndexParams(RequestData requestData) throws ErrorResponseException {
    checkParamsEncoding(requestData.request);
    Map<String, List<String>> params = InternetTestHelpers.extractAllParams(requestData.request);

    ParameterInfo receivingHeiId = ParameterInfo.readParam(params, "receiving_hei_id");
    ParameterInfo sendingHeiId = ParameterInfo.readParam(params, "sending_hei_id");
    ParameterInfo modifiedSince = ParameterInfo.readParam(params, "modified_since");

    requestData.receivingHeiId = receivingHeiId.firstValueOrNull;
    requestData.sendingHeiIds = sendingHeiId.allValues;
    requestData.modifiedSinceString = modifiedSince.firstValueOrNull;

    if (params.size() == 0) {
      errorNoParams(requestData);
    }
    if (!receivingHeiId.hasAny) {
      errorNoReceivingHeiId(requestData);
    }
    if (receivingHeiId.hasMultiple) {
      errorMultipleReceivingHeiIds(requestData);
    }
    if (!sendingHeiId.hasAny) {
      handleNoSendingHeiId(requestData);
    }
    if (sendingHeiId.hasMultiple) {
      handleMultipleSendingHeiId(requestData);
    }
    if (modifiedSince.hasMultiple) {
      errorMultipleModifiedSince(requestData);
    }

    if (requestData.modifiedSinceString != null && requestData.modifiedSince == null) {
      requestData.modifiedSince = parseModifiedSince(requestData.modifiedSinceString);
      if (requestData.modifiedSince == null) {
        errorInvalidModifiedSince(requestData);
      }
    }

    int expectedParams = 0;
    expectedParams += receivingHeiId.coveredParameters;
    expectedParams += sendingHeiId.coveredParameters;
    expectedParams += modifiedSince.coveredParameters;
    if (params.size() > expectedParams) {
      handleUnexpectedParams(requestData);
    }

    if (requestData.receivingHeiId == null || requestData.sendingHeiIds == null) {
      //We expect all of above members to have any value even in invalid scenarios.
      throw new NullPointerException();
    }
  }

  private void extractGetParams(RequestData requestData) throws ErrorResponseException {
    checkParamsEncoding(requestData.request);
    Map<String, List<String>> params = InternetTestHelpers.extractAllParams(requestData.request);

    ParameterInfo receivingHeiId = ParameterInfo.readParam(params, "receiving_hei_id");
    ParameterInfo omobilityIds = ParameterInfo.readParam(params, "omobility_id");

    requestData.receivingHeiId = receivingHeiId.firstValueOrNull;
    requestData.omobilityIds = omobilityIds.allValues;

    if (params.size() == 0) {
      errorNoParams(requestData);
    }
    if (!receivingHeiId.hasAny) {
      errorNoReceivingHeiId(requestData);
    }
    if (receivingHeiId.hasMultiple) {
      errorMultipleReceivingHeiIds(requestData);
    }
    if (!omobilityIds.hasAny) {
      errorNoOMobilityIds(requestData);
    }

    int expectedParams = 0;
    expectedParams += receivingHeiId.coveredParameters;
    expectedParams += omobilityIds.coveredParameters;
    if (params.size() > expectedParams) {
      handleUnexpectedParams(requestData);
    }

    if (requestData.receivingHeiId == null || requestData.omobilityIds.isEmpty()) {
      //We expect all of above members to have any value even in invalid scenarios.
      throw new NullPointerException();
    }
  }

  protected void checkSendingHeiId(RequestData requestData) throws ErrorResponseException {
    List<String> knownSendingHeiIds = tors.stream().map(tor -> tor.sending_hei_id).collect(
        Collectors.toList());
    if (!knownSendingHeiIds.containsAll(requestData.sendingHeiIds)) {
      handleUnknownSendingHeiId(requestData);
    }
  }

  protected void checkReceivingHeiId(RequestData requestData) throws ErrorResponseException {
    if (tors.stream().noneMatch(tor -> tor.receiving_hei_id.equals(requestData.receivingHeiId))) {
      handleUnknownReceivingHeiId(requestData);
    }
  }

  protected void checkOMobilityIds(RequestData requestData) throws ErrorResponseException {
    if (requestData.omobilityIds.size() > getMaxOmobilityIds()) {
      errorMaxOMobilityIdsExceeded(requestData);
    }

    List<String> knownOMobilityIds = tors.stream().map(tor -> tor.tor.getOmobilityId()).collect(
        Collectors.toList());
    if (!knownOMobilityIds.containsAll(requestData.omobilityIds)) {
      handleUnknownOMobilityId(requestData);
    }
  }

  protected List<IMobilityTorEntry> filterIMobilityTorsByModifiedSince(
      List<IMobilityTorEntry> selectedIMobilityTors, RequestData requestData) {
    if (requestData.modifiedSince == null) {
      return selectedIMobilityTors;
    }
    Instant modifiedAt = Instant.from(
        ZonedDateTime.of(2019, 6, 10, 18, 52, 32, 0, ZoneId.of("Z"))
    );
    if (requestData.modifiedSince.toInstant().isAfter(modifiedAt)) {
      return new ArrayList<>();
    } else {
      return selectedIMobilityTors;
    }
  }

  private boolean isIMobilityTorEntrySelectedByRequest(IMobilityTorEntry tor,
      RequestData requestData) {
    boolean receivingMatches = requestData.receivingHeiId.equals(tor.receiving_hei_id);

    boolean sendingMatches = false;
    if (requestData.sendingHeiIds.isEmpty()) {
      sendingMatches = true;
    } else if (requestData.sendingHeiIds.contains(tor.sending_hei_id)) {
      sendingMatches = true;
    }

    return receivingMatches && sendingMatches;
  }

  protected List<IMobilityTorEntry> filterIMobilityTorsForIndex(
      List<IMobilityTorEntry> tors, RequestData requestData) {
    return tors.stream().filter(tor -> isIMobilityTorEntrySelectedByRequest(tor, requestData))
        .collect(Collectors.toList());
  }

  protected boolean filterIMoblityTorForGet(IMobilityTorEntry tor, RequestData requestData) {
    return requestData.receivingHeiId.equals(tor.receiving_hei_id)
        && requestData.omobilityIds.contains(tor.tor.getOmobilityId());
  }

  protected List<IMobilityTorEntry> filterIMobilityTorsForGet(
      List<IMobilityTorEntry> tors, RequestData requestData) {
    return tors.stream().filter(tor -> filterIMoblityTorForGet(tor, requestData))
        .collect(Collectors.toList());
  }

  private List<String> mapToOMobilityIds(List<IMobilityTorEntry> selectedIMobilityTors) {
    return selectedIMobilityTors.stream().map(tor -> tor.tor.getOmobilityId())
        .collect(Collectors.toList());
  }

  protected void errorMaxOMobilityIdsExceeded(
      RequestData requestData) throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(requestData.request, 400, "max-omobility-ids exceeded")
    );
  }

  protected void errorNoOMobilityIds(RequestData requestData) throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(requestData.request, 400, "Neither iia_id nor iia_code provided")
    );
  }

  protected void errorNoParams(RequestData requestData) throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(requestData.request, 400, "No parameters provided")
    );
  }

  protected void errorNoReceivingHeiId(RequestData requestData) throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(requestData.request, 400, "No receiving_hei_id parameter")
    );
  }

  protected void errorMultipleReceivingHeiIds(
      RequestData requestData) throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(requestData.request, 400, "More that one receiving_hei_id provided.")
    );
  }

  protected void handleMultipleSendingHeiId(RequestData requestData) throws ErrorResponseException {
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

  protected void handleUnexpectedParams(RequestData requestData) throws ErrorResponseException {
    //Ignore
  }

  protected void handleUnknownOMobilityId(RequestData requestData) throws ErrorResponseException {
    //Ignore
  }

  protected void handleUnknownReceivingHeiId(
      RequestData requestData) throws ErrorResponseException {
    //Ignore
  }

  protected void handleUnknownSendingHeiId(RequestData requestData) throws ErrorResponseException {
    //Ignore
  }

  protected void handleNoSendingHeiId(RequestData requestData) throws ErrorResponseException {
    // ignore
  }


  static class RequestData {
    public String receivingHeiId;
    public List<String> sendingHeiIds;
    public ZonedDateTime modifiedSince;
    public String modifiedSinceString;
    public List<String> omobilityIds;
    Request request;

    RequestData(Request request) {
      this.request = request;
    }

  }
}
