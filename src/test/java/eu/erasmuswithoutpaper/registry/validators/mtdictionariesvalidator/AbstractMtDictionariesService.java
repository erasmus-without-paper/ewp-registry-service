package eu.erasmuswithoutpaper.registry.validators.mtdictionariesvalidator;

import java.io.IOException;
import java.util.List;

import eu.erasmuswithoutpaper.registry.internet.Request;
import eu.erasmuswithoutpaper.registry.internet.Response;
import eu.erasmuswithoutpaper.registry.validators.AbstractApiService;
import eu.erasmuswithoutpaper.registry.validators.types.MtDictionariesResponse;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

public abstract class AbstractMtDictionariesService extends AbstractApiService {
  protected final String myEndpoint;

  /**
   * @param url
   *     The endpoint at which to listen for requests.
   * @param registryClient
   *     Initialized and refreshed {@link RegistryClient} instance.
   */
  public AbstractMtDictionariesService(String url, RegistryClient registryClient) {
    super(registryClient);
    this.myEndpoint = url;
  }

  @Override
  public Response handleInternetRequest(Request request) throws IOException {
    try {
      if (request.getUrl().startsWith(this.myEndpoint)) {
        return handleMtDictionariesRequest(request);
      }
      return null;
    } catch (ErrorResponseException e) {
      return e.response;
    }
  }

  protected Response createMtDictionariesReponse(List<MtDictionariesResponse.Term> data) {
    MtDictionariesResponse response = new MtDictionariesResponse();
    response.getTerm().addAll(data);
    return marshallResponse(200, response);
  }

  protected abstract Response handleMtDictionariesRequest(Request request)
      throws IOException, ErrorResponseException;
}
