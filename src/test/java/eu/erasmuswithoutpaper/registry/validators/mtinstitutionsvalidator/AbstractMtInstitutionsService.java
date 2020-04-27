package eu.erasmuswithoutpaper.registry.validators.mtinstitutionsvalidator;

import java.io.IOException;
import java.util.List;

import eu.erasmuswithoutpaper.registry.internet.Request;
import eu.erasmuswithoutpaper.registry.internet.Response;
import eu.erasmuswithoutpaper.registry.validators.AbstractApiService;
import eu.erasmuswithoutpaper.registry.validators.types.MtInstitutionsResponse;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

public abstract class AbstractMtInstitutionsService extends AbstractApiService {
  protected final String myEndpoint;

  /**
   * @param url
   *     The endpoint at which to listen for requests.
   * @param registryClient
   *     Initialized and refreshed {@link RegistryClient} instance.
   */
  public AbstractMtInstitutionsService(String url, RegistryClient registryClient) {
    super(registryClient);
    this.myEndpoint = url;
  }

  @Override
  public Response handleInternetRequest(Request request) throws IOException {
    try {
      if (request.getUrl().startsWith(this.myEndpoint)) {
        return handleMtInstitutionsRequest(request);
      }
      return null;
    } catch (ErrorResponseException e) {
      return e.response;
    }
  }

  protected Response createMtInstitutionsReponse(List<MtInstitutionsResponse.Hei> data) {
    MtInstitutionsResponse response = new MtInstitutionsResponse();
    response.getHei().addAll(data);
    return marshallResponse(200, response);
  }

  protected abstract Response handleMtInstitutionsRequest(Request request)
      throws IOException, ErrorResponseException;
}
