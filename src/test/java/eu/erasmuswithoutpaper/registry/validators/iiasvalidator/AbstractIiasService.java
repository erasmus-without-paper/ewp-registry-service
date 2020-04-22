package eu.erasmuswithoutpaper.registry.validators.iiasvalidator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import eu.erasmuswithoutpaper.registry.internet.Request;
import eu.erasmuswithoutpaper.registry.internet.Response;
import eu.erasmuswithoutpaper.registry.validators.AbstractApiService;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

public abstract class AbstractIiasService extends AbstractApiService {
  protected final RegistryClient registryClient;
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
    this.myIndexUrl = indexUrl;
    this.myGetUrl = getUrl;
    this.registryClient = registryClient;
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

  protected abstract Response handleIiasIndexRequest(Request request)
      throws IOException, ErrorResponseException;

  protected abstract Response handleIiasGetRequest(Request request)
      throws IOException, ErrorResponseException;

  protected void checkRequestMethod(Request request) throws ErrorResponseException {
    if (!(request.getMethod().equals("GET") || request.getMethod().equals("POST"))) {
      throw new ErrorResponseException(
          this.createErrorResponse(request, 405, "We expect GETs and POSTs only")
      );
    }
  }

  protected void checkParamsEncoding(Request request) throws ErrorResponseException {
    if (request.getMethod().equals("POST")
        && !request.getHeader("content-type").equals("application/x-www-form-urlencoded")) {
      throw new ErrorResponseException(
          createErrorResponse(request, 415, "Unsupported content-type")
      );
    }
  }

  public static class ParameterInfo {
    public static ParameterInfo readParam(Map<String, List<String>> params, String param) {
      List<String> allValues = params.getOrDefault(param, new ArrayList<>());
      return new ParameterInfo(
          allValues,
          allValues.size() > 0,
          allValues.size() > 1,
          allValues.isEmpty() ? null : allValues.get(0),
          allValues.isEmpty() ? 0 : 1
      );
    }

    private ParameterInfo(List<String> allValues, boolean hasAny, boolean hasMultiple,
        String firstValueOrNull, int coveredParameters) {
      this.hasAny = hasAny;
      this.hasMultiple = hasMultiple;
      this.allValues = allValues;
      this.firstValueOrNull = firstValueOrNull;
      this.coveredParameters = coveredParameters;
    }

    public final boolean hasAny;
    public final boolean hasMultiple;
    public final List<String> allValues;
    public final String firstValueOrNull;
    public final int coveredParameters;
  }
}
