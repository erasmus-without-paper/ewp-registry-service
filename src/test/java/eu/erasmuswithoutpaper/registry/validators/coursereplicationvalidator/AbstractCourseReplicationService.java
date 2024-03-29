package eu.erasmuswithoutpaper.registry.validators.coursereplicationvalidator;

import java.io.IOException;
import java.util.List;

import eu.erasmuswithoutpaper.registry.internet.Request;
import eu.erasmuswithoutpaper.registry.internet.Response;
import eu.erasmuswithoutpaper.registry.validators.AbstractApiService;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

import https.github_com.erasmus_without_paper.ewp_specs_api_course_replication.tree.stable_v1.CourseReplicationResponse;

public abstract class AbstractCourseReplicationService extends AbstractApiService {
  protected final String myEndpoint;

  /**
   * @param url
   *     The endpoint at which to listen for requests.
   * @param registryClient
   *     Initialized and refreshed {@link RegistryClient} instance.
   */
  public AbstractCourseReplicationService(String url, RegistryClient registryClient) {
    super(registryClient);
    this.myEndpoint = url;
  }

  public String getEndpoint() {
    return myEndpoint;
  }

  @Override
  public Response handleInternetRequest(Request request) throws IOException {
    try {
      return this.handleCourseReplicationInternetRequest(request);
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
  protected abstract Response handleCourseReplicationInternetRequest(Request request)
      throws ErrorResponseException;

  protected Response createCourseReplicationResponse(List<String> losIds) {
    CourseReplicationResponse response = new CourseReplicationResponse();
    response.getLosId().addAll(losIds);
    return marshallResponse(200, response);
  }
}
