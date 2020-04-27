package eu.erasmuswithoutpaper.registry.validators.ounitsvalidator;

import java.io.IOException;
import java.util.List;

import eu.erasmuswithoutpaper.registry.internet.Request;
import eu.erasmuswithoutpaper.registry.internet.Response;
import eu.erasmuswithoutpaper.registry.validators.AbstractApiService;
import eu.erasmuswithoutpaper.registry.validators.institutionsvalidator.AbstractInstitutionService;
import eu.erasmuswithoutpaper.registry.validators.types.OunitsResponse;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

public abstract class AbstractOUnitsService extends AbstractApiService {
  protected final String myEndpoint;
  protected final AbstractInstitutionService institutionsService;

  /**
   * @param url The endpoint at which to listen for requests.
   * @param registryClient Initialized and refreshed {@link RegistryClient} instance.
   */
  public AbstractOUnitsService(String url, RegistryClient registryClient,
      AbstractInstitutionService institutionService) {
    super(registryClient);
    this.myEndpoint = url;
    this.institutionsService = institutionService;
  }

  @Override
  public Response handleInternetRequest(Request request) throws IOException {
    try {
      if (request.getUrl().startsWith(this.myEndpoint)) {
        return handleOUnitsInternetRequest(request);
      } else if (request.getUrl().startsWith(institutionsService.getEndpoint())) {
        return institutionsService.handleInternetRequest(request);
      }
      return null;
    } catch (ErrorResponseException e) {
      return e.response;
    }
  }

  protected Response createOUnitsResponse(List<OunitsResponse.Ounit> data) {
    OunitsResponse response = new OunitsResponse();
    response.getOunit().addAll(data);
    return marshallResponse(200, response);
  }

  protected abstract Response handleOUnitsInternetRequest(Request request)
      throws IOException, ErrorResponseException;
}
