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

  protected abstract Response handleIiasIndexRequest(Request request)
      throws IOException, ErrorResponseException;

  protected abstract Response handleIiasGetRequest(Request request)
      throws IOException, ErrorResponseException;
}
