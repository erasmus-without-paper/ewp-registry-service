package eu.erasmuswithoutpaper.registry.validators.imobilitytorsvalidator;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import eu.erasmuswithoutpaper.registry.internet.InternetTestHelpers;
import eu.erasmuswithoutpaper.registry.internet.Request;
import eu.erasmuswithoutpaper.registry.internet.sec.EwpClientWithRsaKey;
import eu.erasmuswithoutpaper.registry.validators.ParameterInfo;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

import org.springframework.core.io.ResourceLoader;


public abstract class IMobilityTorsServiceValidCommon extends AbstractIMobilityTorsServiceCommon {
  /**
   * @param indexUrl       The endpoint at which to listen for requests.
   * @param getUrl         The endpoint at which to listen for requests.
   * @param registryClient Initialized and refreshed {@link RegistryClient} instance.
   */
  public IMobilityTorsServiceValidCommon(String indexUrl, String getUrl, RegistryClient registryClient) {
    super(indexUrl, getUrl, registryClient);
  }

  @SuppressWarnings("unchecked")
  protected static <T> T readFromFile(String filename, Class<T> clazz,
      ResourceLoader resourceLoader) {
    try {
      JAXBContext jc = JAXBContext.newInstance(clazz);
      T parsedRespone = (T) jc.createUnmarshaller()
          .unmarshal(
              resourceLoader.getResource("classpath:test-files/" + filename).getInputStream());
      return parsedRespone;
    } catch (JAXBException | IOException e) {
      throw new RuntimeException(e);
    }
  }

  protected int getMaxOmobilityIds() {
    return 3;
  }

  protected boolean isCallerPermittedToSeeSendingHeiId(
      RequestData requestData, String sendingHeiId) {
    return this.registryClient.getHeisCoveredByClientKey(requestData.client.getRsaPublicKey())
        .contains(sendingHeiId);
  }

  protected void extractIndexParams(RequestData requestData) throws ErrorResponseException {
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

  protected void extractGetParams(RequestData requestData) throws ErrorResponseException {
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
    EwpClientWithRsaKey client;

    RequestData(Request request, EwpClientWithRsaKey client) {
      this.request = request;
      this.client = client;
    }

  }
}
