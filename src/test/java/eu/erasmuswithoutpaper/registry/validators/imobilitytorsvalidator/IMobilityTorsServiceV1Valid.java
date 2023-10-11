package eu.erasmuswithoutpaper.registry.validators.imobilitytorsvalidator;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import eu.erasmuswithoutpaper.registry.internet.Request;
import eu.erasmuswithoutpaper.registry.internet.Response;
import eu.erasmuswithoutpaper.registry.internet.sec.EwpClientWithRsaKey;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

import org.springframework.core.io.ResourceLoader;

import https.github_com.erasmus_without_paper.ewp_specs_api_imobility_tors.blob.stable_v1.endpoints.get_response.ImobilityTorsGetResponse;
import https.github_com.erasmus_without_paper.ewp_specs_api_imobility_tors.blob.stable_v1.endpoints.index_response.ImobilityTorsIndexResponse;

public class IMobilityTorsServiceV1Valid extends IMobilityTorsServiceValidCommon {
  protected List<IMobilityTorEntry> tors = new ArrayList<>();


  /**
   * @param indexUrl       The endpoint at which to listen for requests.
   * @param getUrl         The endpoint at which to listen for requests.
   * @param registryClient Initialized and refreshed {@link RegistryClient} instance.
   */
  public IMobilityTorsServiceV1Valid(String indexUrl, String getUrl, RegistryClient registryClient,
      ResourceLoader resourceLoader) {
    super(indexUrl, getUrl, registryClient);
    fillDataBase(resourceLoader);
  }

  private void fillDataBase(ResourceLoader resourceLoader) {
    final String RECEIVING_HEI_ID = "test.hei01.uw.edu.pl";
    final String SENDING_HEI_ID_1 = "validator-hei01.developers.erasmuswithoutpaper.eu";
    final String SENDING_HEI_ID_2 = "uw.edu.pl";

    String filename = "imobilitytorsvalidator/tor-v1.xml";
    ImobilityTorsGetResponse torsGetResponse = readFromFile(filename,
        ImobilityTorsGetResponse.class, resourceLoader);
    List<ImobilityTorsGetResponse.Tor> tor = torsGetResponse.getTor();
    tors.add(new IMobilityTorEntry(tor.get(0), RECEIVING_HEI_ID, SENDING_HEI_ID_1));
    tors.add(new IMobilityTorEntry(tor.get(1), RECEIVING_HEI_ID, SENDING_HEI_ID_2));
  }

  protected Response createIMobilityTorsGetResponse(List<ImobilityTorsGetResponse.Tor> data) {
    ImobilityTorsGetResponse response = new ImobilityTorsGetResponse();
    response.getTor().addAll(data);
    return marshallResponse(200, response);
  }

  protected Response createIMobilityTorsIndexResponse(List<String> data) {
    ImobilityTorsIndexResponse response = new ImobilityTorsIndexResponse();
    response.getOmobilityId().addAll(data);
    return marshallResponse(200, response);
  }

  @Override
  protected Response handleIMobilityTorsIndexRequest(
      Request request) throws ErrorResponseException {
    try {
      EwpClientWithRsaKey connectedClient = verifyCertificate(request);
      checkRequestMethod(request);

      RequestData requestData = new RequestData(request, connectedClient);
      extractIndexParams(requestData);
      checkReceivingHeiId(requestData);
      checkSendingHeiId(requestData);
      List<IMobilityTorEntry> selectedIMobilityTors = filterIMobilityTorsForIndex(tors,
          requestData);
      selectedIMobilityTors = filterNotPermittedIMobilityTors(selectedIMobilityTors,
          requestData);
      selectedIMobilityTors = filterIMobilityTorsByModifiedSince(selectedIMobilityTors,
          requestData);
      List<String> resultOMobilities = mapToOMobilityIds(selectedIMobilityTors);
      return createIMobilityTorsIndexResponse(resultOMobilities);
    } catch (ErrorResponseException e) {
      return e.response;
    }
  }

  private List<IMobilityTorEntry> filterNotPermittedIMobilityTors(
      List<IMobilityTorEntry> selectedIMobilityTors, RequestData requestData) {
    return selectedIMobilityTors.stream()
        .filter(tor -> isCallerPermittedToSeeSendingHeiId(requestData, tor.sending_hei_id))
        .collect(Collectors.toList());
  }

  @Override
  protected Response handleIMobilityTorsGetRequest(Request request) throws ErrorResponseException {
    try {
      EwpClientWithRsaKey connectedClient = verifyCertificate(request);
      checkRequestMethod(request);

      RequestData requestData = new RequestData(request, connectedClient);
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

  protected static class IMobilityTorEntry {
    public ImobilityTorsGetResponse.Tor tor;
    public String receiving_hei_id;
    public String sending_hei_id;

    public IMobilityTorEntry(ImobilityTorsGetResponse.Tor tor, String receiving_hei_id,
        String sending_hei_id) {
      this.tor = tor;
      this.receiving_hei_id = receiving_hei_id;
      this.sending_hei_id = sending_hei_id;
    }
  }
}
