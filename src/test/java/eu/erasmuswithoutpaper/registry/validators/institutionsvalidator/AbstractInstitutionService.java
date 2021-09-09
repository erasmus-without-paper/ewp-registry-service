package eu.erasmuswithoutpaper.registry.validators.institutionsvalidator;

import java.io.IOException;
import java.util.List;

import eu.erasmuswithoutpaper.registry.internet.Request;
import eu.erasmuswithoutpaper.registry.internet.Response;
import eu.erasmuswithoutpaper.registry.validators.AbstractApiService;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

import https.github_com.erasmus_without_paper.ewp_specs_api_institutions.tree.stable_v2.InstitutionsResponse;

public abstract class AbstractInstitutionService extends AbstractApiService {
  protected final String myEndpoint;

  /**
   * @param url
   *     The endpoint at which to listen for requests.
   * @param registryClient
   *     Initialized and refreshed {@link RegistryClient} instance.
   */
  public AbstractInstitutionService(String url, RegistryClient registryClient) {
    super(registryClient);
    this.myEndpoint = url;
  }

  public String getEndpoint() {
    return myEndpoint;
  }

  @Override
  public Response handleInternetRequest(Request request) throws IOException {
    try {
      return this.handleInstitutionsInternetRequest(request);
    } catch (ErrorResponseException e) {
      return e.response;
    }
  }

  protected Response createInstitutionsResponse(List<InstitutionsResponse.Hei> heis) {
    InstitutionsResponse response = new InstitutionsResponse();
    response.getHei().addAll(heis);
    return marshallResponse(200, response);
  }

  /**
   * @param request
   *     The request for which a response is to be generated
   * @return {@link Response} object.
   * @throws ErrorResponseException
   *     This can be thrown instead of returning the error response (a shortcut).
   */
  protected abstract Response handleInstitutionsInternetRequest(Request request)
      throws ErrorResponseException;
}
