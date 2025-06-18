package eu.erasmuswithoutpaper.registry.validators.omobilities;

import java.io.IOException;

import eu.erasmuswithoutpaper.registry.internet.Request;
import eu.erasmuswithoutpaper.registry.internet.Response;
import eu.erasmuswithoutpaper.registry.validators.AbstractApiService;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

public abstract class AbstractOMobilitiesService extends AbstractApiService {
  protected final RegistryClient registryClient;
  private final String myIndexUrl;
  private final String myGetUrl;
  private final String myUpdateUrl;

  /**
   * @param indexUrl The endpoint at which to listen for INDEX requests.
   * @param getUrl The endpoint at which to listen for GET requests.
   * @param updateUrl The endpoint at which to listen for UPDATE requests.
   * @param registryClient Initialized and refreshed {@link RegistryClient} instance.
   */
  public AbstractOMobilitiesService(String indexUrl, String getUrl, String updateUrl,
      RegistryClient registryClient) {
    super(registryClient);
    this.myIndexUrl = indexUrl;
    this.myGetUrl = getUrl;
    this.myUpdateUrl = updateUrl;
    this.registryClient = registryClient;
  }

  @Override
  public Response handleInternetRequest(Request request) throws IOException {
    try {
      if (request.getUrl().startsWith(this.myGetUrl)) {
        return handleOMobilitiesGetRequest(request);
      } else if (request.getUrl().startsWith(this.myIndexUrl)) {
        return handleOMobilitiesIndexRequest(request);
      } else if (request.getUrl().startsWith(this.myUpdateUrl)) {
        return handleOMobilitiesUpdateRequest(request);
      }
      return null;
    } catch (ErrorResponseException e) {
      return e.response;
    }
  }

  protected abstract Response handleOMobilitiesIndexRequest(Request request)
      throws IOException, ErrorResponseException;

  protected abstract Response handleOMobilitiesGetRequest(Request request)
      throws IOException, ErrorResponseException;

  protected Response handleOMobilitiesUpdateRequest(Request request) throws ErrorResponseException {
    throw new RuntimeException("Not implemented yet.");
  }
}
