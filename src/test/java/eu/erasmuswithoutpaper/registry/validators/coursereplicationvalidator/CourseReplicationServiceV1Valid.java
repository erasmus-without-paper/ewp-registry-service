package eu.erasmuswithoutpaper.registry.validators.coursereplicationvalidator;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import eu.erasmuswithoutpaper.registry.internet.InternetTestHelpers;
import eu.erasmuswithoutpaper.registry.internet.Request;
import eu.erasmuswithoutpaper.registry.internet.Response;
import eu.erasmuswithoutpaper.registry.internet.sec.EwpHttpSigRequestAuthorizer;
import eu.erasmuswithoutpaper.registry.internet.sec.Http4xx;
import eu.erasmuswithoutpaper.registry.validators.ValidatorKeyStore;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

public class CourseReplicationServiceV1Valid extends AbstractCourseReplicationService {

  protected final List<String> coveredHeiIds;
  private final EwpHttpSigRequestAuthorizer myAuthorizer;
  private final List<String> coveredLosIds;


  public CourseReplicationServiceV1Valid(String url, RegistryClient registryClient,
      ValidatorKeyStore validatorKeyStore) {
    super(url, registryClient);
    this.myAuthorizer = new EwpHttpSigRequestAuthorizer(this.registryClient);
    coveredHeiIds = validatorKeyStore.getCoveredHeiIDs();
    coveredLosIds = createFakeHeiData();
  }

  public List<String> getCoveredLosIds() {
    return coveredLosIds;
  }

  protected List<String> createFakeHeiData() {
    return Arrays.asList("CR/123", "CR/5151", "CR/12123");
  }

  public List<String> getCoveredHeiIds() {
    return coveredHeiIds;
  }

  @Override
  public Response handleCourseReplicationInternetRequest(Request request) {
    if (!request.getUrl().startsWith(this.myEndpoint)) {
      return null;
    }
    try {
      RequestData requestData = new RequestData(request);
      verifyCertificate(requestData);
      checkRequestMethod(requestData);
      extractParams(requestData);
      checkModifiedSince(requestData);
      List<String> losIds = processHei(requestData);
      return createCourseReplicationResponse(losIds);
    } catch (ErrorResponseException e) {
      return e.response;
    }
  }

  private void checkModifiedSince(RequestData requestData)
      throws ErrorResponseException {
    if (requestData.requestedModifiedSince != null) {
      ZonedDateTime modifiedSince = parseModifiedSince(requestData.requestedModifiedSince);
      if (modifiedSince == null) {
        errorInvalidModifiedSince(requestData);
      }
      requestData.requestedModifiedSinceDate = modifiedSince;
    }
  }

  private void verifyCertificate(RequestData requestData) throws ErrorResponseException {
    try {
      this.myAuthorizer.authorize(requestData.request);
    } catch (Http4xx e) {
      throw new ErrorResponseException(e.generateEwpErrorResponse());
    }
  }

  private void extractParams(RequestData requestData) throws ErrorResponseException {
    checkParamsEncoding(requestData);
    Map<String, List<String>> params = InternetTestHelpers.extractAllParams(requestData.request);

    requestData.requestedHeiIds = params.getOrDefault("hei_id", new ArrayList<>());
    requestData.requestedModifiedSinces = params.getOrDefault("modified_since", new ArrayList<>());
    boolean hasHeiId = requestData.requestedHeiIds.size() > 0;
    boolean multipleHeiId = requestData.requestedHeiIds.size() > 1;
    boolean hasModifiedSince = requestData.requestedModifiedSinces.size() > 0;
    boolean multipleModifiedSince = requestData.requestedModifiedSinces.size() > 1;

    int count_params = 0;

    if (hasHeiId) {
      requestData.requestedHeiId = requestData.requestedHeiIds.get(0);
      count_params++;
    }

    requestData.requestedModifiedSince = null;
    if (hasModifiedSince) {
      requestData.requestedModifiedSince = requestData.requestedModifiedSinces.get(0);
      count_params++;
    }

    if (!hasHeiId && !hasModifiedSince) {
      errorNoParameters(requestData, params);
    }

    if (multipleHeiId) {
      errorMultipleHeiIds(requestData);
    }

    if (multipleModifiedSince) {
      errorMultipleModifiedSince(requestData);
    }

    if (!hasHeiId) {
      errorNoHeiId(requestData, params);
    }

    if (params.size() > count_params) {
      errorAdditionalParameters(requestData, params);
    }

    if (requestData.requestedHeiId == null) {
      throw new NullPointerException();
    }
  }

  private void errorMultipleModifiedSince(RequestData requestData) throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(requestData.request, 400, "Multiple modified_since params."));
  }

  protected void errorMultipleHeiIds(RequestData requestData) throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(requestData.request, 400, "Multiple HEI IDs."));
  }

  protected void checkParamsEncoding(RequestData requestData) throws ErrorResponseException {
    if (requestData.request.getMethod().equals("POST")
        && !requestData.request.getHeader("content-type")
        .equals("application/x-www-form-urlencoded")) {
      throw new ErrorResponseException(
          createErrorResponse(requestData.request, 415, "Unsupported content-type"));
    }
  }

  protected void errorAdditionalParameters(RequestData requestData,
      Map<String, List<String>> params)
      throws ErrorResponseException {
    //Ignore unknown parameters
  }

  protected void errorNoHeiId(RequestData requestData, Map<String, List<String>> params)
      throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(requestData.request, 400, "Expected \"hei_id\" parameters"));
  }

  protected void errorNoParameters(RequestData requestData, Map<String, List<String>> params)
      throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(requestData.request, 400, "No parameters provided"));
  }

  protected void errorInvalidModifiedSince(RequestData requestData) throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(requestData.request, 400, "Invalid modified_since format."));
  }

  protected void checkRequestMethod(RequestData requestData) throws ErrorResponseException {
    if (!(requestData.request.getMethod().equals("GET")
        || requestData.request.getMethod().equals("POST"))) {
      throw new ErrorResponseException(
          this.createErrorResponse(requestData.request, 405, "We expect GETs and POSTs only"));
    }
  }

  protected List<String> processCoveredHei(RequestData requestData)
      throws ErrorResponseException {
    if (requestData.requestedModifiedSinceDate != null
        && requestData.requestedModifiedSinceDate.isAfter(ZonedDateTime.now())) {
      return new ArrayList<>();
    }
    return coveredLosIds;
  }

  protected List<String> processNotCoveredHei(RequestData requestData)
      throws ErrorResponseException {
    throw new ErrorResponseException(
        this.createErrorResponse(requestData.request, 400, "Unknown HEI ID."));
  }

  protected List<String> processHei(RequestData requestData)
      throws ErrorResponseException {
    if (coveredHeiIds.contains(requestData.requestedHeiId)) {
      return processCoveredHei(requestData);
    } else {
      return processNotCoveredHei(requestData);
    }
  }


  protected static class RequestData {
    Request request;
    String requestedHeiId;
    String requestedModifiedSince;
    List<String> requestedHeiIds;
    List<String> requestedModifiedSinces;
    ZonedDateTime requestedModifiedSinceDate;

    public RequestData(Request request) {
      this.request = request;
    }
  }
}
