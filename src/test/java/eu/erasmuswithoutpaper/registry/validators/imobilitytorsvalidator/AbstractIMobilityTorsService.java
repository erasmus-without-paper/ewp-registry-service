package eu.erasmuswithoutpaper.registry.validators.imobilitytorsvalidator;

import java.io.IOException;
import java.util.List;

import eu.erasmuswithoutpaper.registry.internet.Request;
import eu.erasmuswithoutpaper.registry.internet.Response;
import eu.erasmuswithoutpaper.registry.validators.AbstractApiService;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

import https.github_com.erasmus_without_paper.ewp_specs_api_imobility_tors.blob.stable_v1.endpoints.get_response.ImobilityTorsGetResponse;
import https.github_com.erasmus_without_paper.ewp_specs_api_imobility_tors.blob.stable_v1.endpoints.index_response.ImobilityTorsIndexResponse;

public abstract class AbstractIMobilityTorsService extends AbstractApiService {
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
  public AbstractIMobilityTorsService(String indexUrl, String getUrl, RegistryClient registryClient) {
    super(registryClient);
    this.myIndexUrl = indexUrl;
    this.myGetUrl = getUrl;
    this.registryClient = registryClient;
  }

  @Override
  public Response handleInternetRequest(Request request) throws IOException {
    try {
      if (request.getUrl().startsWith(this.myGetUrl)) {
        return handleIMobilityTorsGetRequest(request);
      } else if (request.getUrl().startsWith(this.myIndexUrl)) {
        return handleIMobilityTorsIndexRequest(request);
      }
      return null;
    } catch (ErrorResponseException e) {
      return e.response;
    }
  }

  protected Response createIMobilityTorsGetResponse(List<ImobilityTorsGetResponse.Tor> data) {
    ImobilityTorsGetResponse response = new ImobilityTorsGetResponse();
    response.getTor().addAll(data);
    return marshallResponse(200, response);
  }

  protected Response createIMobilityTorsIndexResponse(List<String> data) {
    ImobilityTorsIndexResponse response = new ImobilityTorsIndexResponse();
    response.getOmobilityId().addAll(data);
    return marshallResponse(200, response);
  }

  protected abstract Response handleIMobilityTorsIndexRequest(Request request)
      throws IOException, ErrorResponseException;

  protected abstract Response handleIMobilityTorsGetRequest(Request request)
      throws IOException, ErrorResponseException;
}
