package eu.erasmuswithoutpaper.registry.validators.iiasvalidator;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import eu.erasmuswithoutpaper.registry.internet.Request;
import eu.erasmuswithoutpaper.registry.internet.Response;
import eu.erasmuswithoutpaper.registry.validators.AbstractApiService;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

public abstract class AbstractIiasService extends AbstractApiService {

  static class RequestData {
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

  protected <T> List<T> filterIiasByModifiedSince(List<T> selectedIias, RequestData requestData) {
    if (requestData.modifiedSince == null) {
      return selectedIias;
    }
    Instant modifiedAt = Instant.from(ZonedDateTime.of(2019, 6, 10, 18, 52, 32, 0, ZoneId.of("Z")));
    if (requestData.modifiedSince.toInstant().isAfter(modifiedAt)) {
      return new ArrayList<>();
    } else {
      return selectedIias;
    }
  }

  protected int getMaxIiaIds() {
    return 3;
  }

  protected abstract Response handleIiasIndexRequest(Request request)
      throws IOException, ErrorResponseException;

  protected abstract Response handleIiasGetRequest(Request request)
      throws IOException, ErrorResponseException;

  protected void errorNoParams(RequestData requestData) throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(requestData.request, 400, "No parameters provided"));
  }

  protected void errorMultipleModifiedSince(RequestData requestData) throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(requestData.request, 400, "More that one modified_since provided."));
  }

  protected void errorInvalidModifiedSince(RequestData requestData) throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(requestData.request, 400, "Invalid modified_since format."));
  }

  protected void errorInvalidAcademicYearId(RequestData requestData) throws ErrorResponseException {
    throw new ErrorResponseException(createErrorResponse(requestData.request, 400,
        "receiving_academic_year_id has incorrect format"));
  }

  protected void errorMaxIdsExceeded(RequestData requestData) throws ErrorResponseException {
    throw new ErrorResponseException(
        createErrorResponse(requestData.request, 400, "max-iia-ids exceeded"));
  }

  protected void checkIds(RequestData requestData) throws ErrorResponseException {
    if (requestData.iiaIds.size() > getMaxIiaIds()) {
      errorMaxIdsExceeded(requestData);
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
