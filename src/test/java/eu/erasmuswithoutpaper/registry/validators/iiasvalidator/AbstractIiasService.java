package eu.erasmuswithoutpaper.registry.validators.iiasvalidator;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import eu.erasmuswithoutpaper.registry.internet.InternetTestHelpers;
import eu.erasmuswithoutpaper.registry.internet.Request;
import eu.erasmuswithoutpaper.registry.internet.Response;
import eu.erasmuswithoutpaper.registry.validators.AbstractApiService;
import eu.erasmuswithoutpaper.registry.validators.ParameterInfo;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

public abstract class AbstractIiasService extends AbstractApiService {

  static class RequestData {
    public String partnerHeiId;
    public List<String> receivingAcademicYearIds;
    public ZonedDateTime modifiedSince;
    public List<String> iiaIds;
    Request request;
    String heiId;

    RequestData(Request request) {
      this.request = request;
    }
  }

  protected final List<String> coveredHeiIds = new ArrayList<>();
  protected final List<String> partnersHeiIds = new ArrayList<>();

  private final String myIndexUrl;
  private final String myGetUrl;

  /**
   * @param indexUrl
   *     The endpoint at which to listen for INDEX requests.
   * @param getUrl
   *     The endpoint at which to listen for GET requests.
   * @param registryClient
   *     Initialized and refreshed {@link RegistryClient} instance.
   */
  public AbstractIiasService(String indexUrl, String getUrl, RegistryClient registryClient) {
    super(registryClient);
    this.myIndexUrl = indexUrl;
    this.myGetUrl = getUrl;
  }

  @Override
  public Response handleInternetRequest(Request request) throws IOException {
    try {
      if (request.getUrl().startsWith(this.myGetUrl)) {
        return handleIiasGetRequest(request);
      } else if (request.getUrl().startsWith(this.myIndexUrl)) {
        return handleIiasIndexRequest(request);
      }
      return null;
    } catch (ErrorResponseException e) {
      return e.response;
    }
  }

  protected void extractIndexParams(RequestData requestData) throws ErrorResponseException {
    checkParamsEncoding(requestData.request);
    Map<String, List<String>> params = InternetTestHelpers.extractAllParams(requestData.request);

    ParameterInfo heiId = ParameterInfo.readParam(params, "hei_id");
    ParameterInfo partnerHeiId = ParameterInfo.readParam(params, "partner_hei_id");
    ParameterInfo receivingAcademicYearId =
        ParameterInfo.readParam(params, "receiving_academic_year_id");
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

    if (requestData.heiId == null) {
      // We expect all of above members to have any value even in invalid scenarios.
      throw new NullPointerException();
    }
  }


  protected abstract Response handleIiasIndexRequest(Request request)
      throws IOException, ErrorResponseException;

  protected abstract Response handleIiasGetRequest(Request request)
      throws IOException, ErrorResponseException;

  protected void errorNoParams(RequestData requestData) throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(requestData.request, 400, "No parameters provided"));
  }

  protected void errorNoHeiId(RequestData requestData) throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(requestData.request, 400, "No hei_id parameter"));
  }

  protected void errorMultipleHeiIds(RequestData requestData) throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(requestData.request, 400, "More that one hei_id provided."));
  }

  protected void errorMultiplePartnerHeiId(RequestData requestData) throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(requestData.request, 400, "More that one partner_hei_id provided."));
  }

  protected void errorMultipleModifiedSince(RequestData requestData) throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(requestData.request, 400, "More that one modified_since provided."));
  }

  protected void errorInvalidModifiedSince(RequestData requestData) throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(requestData.request, 400, "Invalid modified_since format."));
  }

  protected void errorUnknownHeiId(RequestData requestData) throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(requestData.request, 400, "Unknown hei_id"));
  }

  protected void errorHeiIdsEqual(RequestData requestData) throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(requestData.request, 400, "hei_id and partner_hei_id are equal"));
  }

  protected void errorInvalidAcademicYearId(RequestData requestData) throws ErrorResponseException {
    throw new ErrorResponseException(createErrorResponse(requestData.request, 400,
        "receiving_academic_year_id has incorrect format"));
  }

  protected void checkPartnerHei(RequestData requestData) throws ErrorResponseException {
    if (requestData.heiId.equals(requestData.partnerHeiId)) {
      errorHeiIdsEqual(requestData);
    }
  }

  protected void checkHei(RequestData requestData) throws ErrorResponseException {
    if (!coveredHeiIds.contains(requestData.heiId)) {
      errorUnknownHeiId(requestData);
    }
  }

  protected void checkReceivingAcademicYearIds(RequestData requestData)
      throws ErrorResponseException {
    for (String receivingAcademicYear : requestData.receivingAcademicYearIds) {
      if (!checkReceivingAcademicYearId(receivingAcademicYear)) {
        errorInvalidAcademicYearId(requestData);
      }
    }
  }

}
