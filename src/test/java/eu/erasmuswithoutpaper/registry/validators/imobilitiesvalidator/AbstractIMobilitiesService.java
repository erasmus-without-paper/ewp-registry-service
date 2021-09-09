package eu.erasmuswithoutpaper.registry.validators.imobilitiesvalidator;

import java.io.IOException;

import eu.erasmuswithoutpaper.registry.internet.Request;
import eu.erasmuswithoutpaper.registry.internet.Response;
import eu.erasmuswithoutpaper.registry.validators.AbstractApiService;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

public abstract class AbstractIMobilitiesService extends AbstractApiService {
  protected final String getUrl;

  /**
   * @param getUrl
   *     The endpoint at which to listen for get requests.
   * @param registryClient
   *     Initialized and refreshed {@link RegistryClient} instance.
   */
  public AbstractIMobilitiesService(String getUrl, RegistryClient registryClient) {
    super(registryClient);
    this.getUrl = getUrl;
  }

  public String getEndpoint() {
    return getUrl;
  }

  @Override
  public Response handleInternetRequest(Request request) throws IOException {
    try {
      if (request.getUrl().startsWith(this.getUrl)) {
        return this.handleIMobilitiesGetInternetRequest(request);
      }
      return null;
    } catch (ErrorResponseException e) {
      return e.response;
    }
  }

  /**
   * @param request
   *     The request for which a response is to be generated
   * @return {@link Response} object.
   * @throws ErrorResponseException
   *     This can be thrown instead of returning the error response (a shortcut).
   */
  protected abstract Response handleIMobilitiesGetInternetRequest(Request request)
      throws ErrorResponseException;
}
