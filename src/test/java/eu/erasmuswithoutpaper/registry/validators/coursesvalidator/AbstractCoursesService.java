package eu.erasmuswithoutpaper.registry.validators.coursesvalidator;

import java.io.IOException;
import java.util.List;

import eu.erasmuswithoutpaper.registry.internet.Request;
import eu.erasmuswithoutpaper.registry.internet.Response;
import eu.erasmuswithoutpaper.registry.validators.AbstractApiService;
import eu.erasmuswithoutpaper.registry.validators.coursereplicationvalidator.AbstractCourseReplicationService;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

import https.github_com.erasmus_without_paper.ewp_specs_api_courses.tree.stable_v1.CoursesResponse;

public abstract class AbstractCoursesService extends AbstractApiService {
  protected final String myEndpoint;
  protected final AbstractCourseReplicationService courseReplicationService;

  /**
   * @param url
   *     The endpoint at which to listen for requests.
   * @param registryClient
   *     Initialized and refreshed {@link RegistryClient} instance.
   */
  public AbstractCoursesService(String url, RegistryClient registryClient,
      AbstractCourseReplicationService courseReplicationService) {
    super(registryClient);
    this.myEndpoint = url;
    this.courseReplicationService = courseReplicationService;
  }

  @Override
  public Response handleInternetRequest(Request request) throws IOException {
    try {
      if (request.getUrl().startsWith(this.myEndpoint)) {
        return handleCoursesInternetRequest(request);
      } else if (request.getUrl().startsWith(courseReplicationService.getEndpoint())) {
        return courseReplicationService.handleInternetRequest(request);
      }
      return null;
    } catch (ErrorResponseException e) {
      return e.response;
    }
  }

  protected Response createCoursesResponse(
      List<CoursesResponse.LearningOpportunitySpecification> data) {
    CoursesResponse response = new CoursesResponse();
    response.getLearningOpportunitySpecification().addAll(data);
    return marshallResponse(200, response);
  }

  protected abstract Response handleCoursesInternetRequest(Request request)
      throws IOException, ErrorResponseException;
}
