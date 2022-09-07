package eu.erasmuswithoutpaper.registry.validators.imobilitytorsvalidator;

import java.util.List;

import eu.erasmuswithoutpaper.registry.internet.Response;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

import https.github_com.erasmus_without_paper.ewp_specs_api_imobility_tors.blob.stable_v1.endpoints.get_response.ImobilityTorsGetResponse;
import https.github_com.erasmus_without_paper.ewp_specs_api_imobility_tors.blob.stable_v1.endpoints.index_response.ImobilityTorsIndexResponse;

public abstract class AbstractIMobilityTorsServiceV1 extends AbstractIMobilityTorsServiceCommon {
  /**
   * @param indexUrl
   *     The endpoint at which to listen for INDEX requests.
   * @param getUrl
   *     The endpoint at which to listen for GET requests.
   * @param registryClient
   *     Initialized and refreshed {@link RegistryClient} instance.
   */
  public AbstractIMobilityTorsServiceV1(String indexUrl, String getUrl, RegistryClient registryClient) {
    super(indexUrl, getUrl, registryClient);
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
}
