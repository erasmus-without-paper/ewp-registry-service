package eu.erasmuswithoutpaper.registry.validators.mtprojectsvalidator;

import java.io.IOException;
import java.util.List;

import eu.erasmuswithoutpaper.registry.internet.Request;
import eu.erasmuswithoutpaper.registry.internet.Response;
import eu.erasmuswithoutpaper.registry.validators.AbstractApiService;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

import https.github_com.erasmus_without_paper.ewp_specs_api_mt_projects.tree.stable_v1.MtProjectsResponse;

public abstract class AbstractMtProjectsService extends AbstractApiService {
  protected final String myEndpoint;

  /**
   * @param url
   *     The endpoint at which to listen for requests.
   * @param registryClient
   *     Initialized and refreshed {@link RegistryClient} instance.
   */
  public AbstractMtProjectsService(String url, RegistryClient registryClient) {
    super(registryClient);
    this.myEndpoint = url;
  }

  @Override
  public Response handleInternetRequest(Request request) throws IOException {
    try {
      if (request.getUrl().startsWith(this.myEndpoint)) {
        return handleMtProjectsRequest(request);
      }
      return null;
    } catch (ErrorResponseException e) {
      return e.response;
    }
  }

  protected Response createMtProjectsReponse(List<MtProjectsResponse.Project> data) {
    MtProjectsResponse response = new MtProjectsResponse();
    response.getProject().addAll(data);
    return marshallResponse(200, response);
  }

  protected abstract Response handleMtProjectsRequest(Request request)
      throws IOException, ErrorResponseException;
}
