package eu.erasmuswithoutpaper.registry.validators.coursereplicationvalidator;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.erasmuswithoutpaper.registry.internet.InternetTestHelpers;
import eu.erasmuswithoutpaper.registry.internet.Request;
import eu.erasmuswithoutpaper.registry.internet.Response;
import eu.erasmuswithoutpaper.registry.internet.sec.EwpHttpSigRequestAuthorizer;
import eu.erasmuswithoutpaper.registry.internet.sec.Http4xx;
import eu.erasmuswithoutpaper.registry.validators.ValidatorKeyStore;
import eu.erasmuswithoutpaper.registry.validators.types.CourseReplicationResponse;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

public class CourseReplicationServiceV2Valid extends AbstractCourseReplicationService {

  private final EwpHttpSigRequestAuthorizer myAuthorizer;

  private final List<String> coveredHeiIds;
  private final List<String> coveredLosIds;

  protected Request currentRequest;
  protected String requestedHeiId;
  protected String requestedModifiedSince;
  protected List<String> requestedHeiIds;
  protected List<String> requestedModifiedSinces;
  protected ZonedDateTime requestedModifiedSinceDate;

  public CourseReplicationServiceV2Valid(String url, RegistryClient registryClient,
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

  public List<String> GetCoveredHeiIds() {
    return coveredHeiIds;
  }

  @Override
  public Response handleCourseReplicationInternetRequest(Request request) {
    if (!request.getUrl().startsWith(this.myEndpoint)) {
      return null;
    }
    try {
      currentRequest = request;
      VerifyCertificate();
      CheckRequestMethod();
      ExtractParams();
      CheckModifiedSince();
      List<String> losIds = ProcessHei();
      return createCourseReplicationResponse(losIds);
    } catch (ErrorResponseException e) {
      return e.response;
    }
  }

  private void CheckModifiedSince()
      throws ErrorResponseException {
    if (this.requestedModifiedSince != null) {
      try {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mmX");
        this.requestedModifiedSinceDate =
            ZonedDateTime.parse(this.requestedModifiedSince, formatter);
      } catch (DateTimeParseException e) {
        ErrorInvalidModifiedSince();
      }
    }
  }

  private Response createCourseReplicationResponse(List<String> losIds) {
    CourseReplicationResponse response = new CourseReplicationResponse();
    response.getLosId().addAll(losIds);
    return marshallResponse(200, response);
  }


  private void VerifyCertificate() throws ErrorResponseException {
    try {
      this.myAuthorizer.authorize(this.currentRequest);
    } catch (Http4xx e) {
      throw new ErrorResponseException(e.generateEwpErrorResponse());
    }
  }

  private void ExtractParams() throws ErrorResponseException {
    CheckParamsEncoding();
    Map<String, List<String>> params = InternetTestHelpers.extractAllParams(this.currentRequest);

    this.requestedHeiIds = params.getOrDefault("hei_id", new ArrayList<>());
    this.requestedModifiedSinces = params.getOrDefault("modified_since", new ArrayList<>());
    boolean hasHeiId = this.requestedHeiIds.size() > 0;
    boolean multipleHeiId = this.requestedHeiIds.size() > 1;
    boolean hasModifiedSince = this.requestedModifiedSinces.size() > 0;
    boolean multipleModifiedSince = this.requestedModifiedSinces.size() > 1;

    int count_params = 0;

    if (hasHeiId) {
      this.requestedHeiId = this.requestedHeiIds.get(0);
      count_params++;
    }

    if (hasModifiedSince) {
      this.requestedModifiedSince = this.requestedModifiedSinces.get(0);
      count_params++;
    }

    if (!hasHeiId && !hasModifiedSince) {
      ErrorNoParameters(params);
    }

    if (multipleHeiId) {
      ErrorMultipleHeiIds();
    }

    if (multipleModifiedSince) {
      ErrorMultipleModifiedSince();
    }

    if (!hasHeiId) {
      ErrorNoHeiId(params);
    }

    if (params.size() > count_params) {
      ErrorAdditionalParameters(params);
    }

    if (this.requestedHeiId == null) {
      throw new NullPointerException();
    }
  }

  private void ErrorMultipleModifiedSince() throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(this.currentRequest, 400, "Multiple modified_since params."));
  }

  private void ErrorMultipleHeiIds() throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(this.currentRequest, 400, "Multiple HEI IDs."));
  }

  protected void CheckParamsEncoding() throws ErrorResponseException {
    if (this.currentRequest.getMethod().equals("POST")
        && !this.currentRequest.getHeader("content-type")
        .equals("application/x-www-form-urlencoded")) {
      throw new ErrorResponseException(
          createErrorResponse(this.currentRequest, 415, "Unsupported content-type"));
    }
  }

  protected void ErrorAdditionalParameters(Map<String, List<String>> params)
      throws ErrorResponseException {
    //Ignore unknown parameters
  }

  protected void ErrorNoHeiId(Map<String, List<String>> params)
      throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(this.currentRequest, 400, "Expected \"hei_id\" parameters"));
  }

  protected void ErrorNoParameters(Map<String, List<String>> params)
      throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(this.currentRequest, 400, "No parameters provided"));
  }

  private void ErrorInvalidModifiedSince() throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(this.currentRequest, 400, "Invalid modified_since format."));
  }

  protected void CheckRequestMethod() throws ErrorResponseException {
    if (!(this.currentRequest.getMethod().equals("GET")
        || this.currentRequest.getMethod().equals("POST"))) {
      throw new ErrorResponseException(
          this.createErrorResponse(this.currentRequest, 405, "We expect GETs and POSTs only"));
    }
  }

  protected List<String> ProcessCoveredHei()
      throws ErrorResponseException {
    if (this.requestedModifiedSinceDate != null
        && this.requestedModifiedSinceDate.isAfter(ZonedDateTime.now())) {
      return new ArrayList<>();
    }
    return coveredLosIds;
  }

  protected List<String> ProcessNotCoveredHei()
      throws ErrorResponseException {
    throw new ErrorResponseException(
        this.createErrorResponse(this.currentRequest, 400, "Unknown HEI ID."));
  }

  protected List<String> ProcessHei()
      throws ErrorResponseException {
    if (coveredHeiIds.contains(this.requestedHeiId)) {
      return ProcessCoveredHei();
    } else {
      return ProcessNotCoveredHei();
    }
  }
}
