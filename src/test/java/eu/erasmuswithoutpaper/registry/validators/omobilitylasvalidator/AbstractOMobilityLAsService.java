package eu.erasmuswithoutpaper.registry.validators.omobilitylasvalidator;

import java.io.IOException;
import java.util.List;

import eu.erasmuswithoutpaper.registry.internet.Request;
import eu.erasmuswithoutpaper.registry.internet.Response;
import eu.erasmuswithoutpaper.registry.validators.AbstractApiService;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

import https.github_com.erasmus_without_paper.ewp_specs_api_omobility_las.blob.stable_v1.endpoints.get_response.LearningAgreement;
import https.github_com.erasmus_without_paper.ewp_specs_api_omobility_las.blob.stable_v1.endpoints.get_response.OmobilityLasGetResponse;
import https.github_com.erasmus_without_paper.ewp_specs_api_omobility_las.blob.stable_v1.endpoints.index_response.OmobilityLasIndexResponse;
import https.github_com.erasmus_without_paper.ewp_specs_api_omobility_las.blob.stable_v1.endpoints.update_response.OmobilityLasUpdateResponse;
import https.github_com.erasmus_without_paper.ewp_specs_architecture.blob.stable_v1.common_types.MultilineStringWithOptionalLang;

public abstract class AbstractOMobilityLAsService extends AbstractApiService {
  protected final RegistryClient registryClient;
  private final String myIndexUrl;
  private final String myGetUrl;
  private final String myUpdateUrl;

  /**
   * @param indexUrl
   *     The endpoint at which to listen for INDEX requests.
   * @param getUrl
   *     The endpoint at which to listen for GET requests.
   * @param updateUrl
   *     The endpoint at which to listen for UPDATE requests.
   * @param registryClient
   *     Initialized and refreshed {@link RegistryClient} instance.
   */
  public AbstractOMobilityLAsService(String indexUrl, String getUrl, String updateUrl,
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

  protected Response createOMobilityLAsGetResponse(
      List<LearningAgreement> data) {
    OmobilityLasGetResponse response = new OmobilityLasGetResponse();
    response.getLa().addAll(data);
    return marshallResponse(200, response);
  }

  protected Response createOMobilityLAsIndexResponse(List<String> data) {
    OmobilityLasIndexResponse response = new OmobilityLasIndexResponse();
    response.getOmobilityId().addAll(data);
    return marshallResponse(200, response);
  }

  protected Response createOMobilityLAsUpdateResponse(String message) {
    OmobilityLasUpdateResponse response = new OmobilityLasUpdateResponse();
    MultilineStringWithOptionalLang multilineString = new MultilineStringWithOptionalLang();
    multilineString.setLang("en");
    multilineString.setValue(message);
    response.getSuccessUserMessage().add(multilineString);
    return marshallResponse(200, response);
  }

  protected abstract Response handleOMobilitiesIndexRequest(Request request)
      throws IOException, ErrorResponseException;

  protected abstract Response handleOMobilitiesGetRequest(Request request)
      throws IOException, ErrorResponseException;

  protected abstract Response handleOMobilitiesUpdateRequest(Request request)
      throws IOException, ErrorResponseException;
}
