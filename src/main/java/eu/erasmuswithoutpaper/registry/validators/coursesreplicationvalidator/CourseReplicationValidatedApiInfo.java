package eu.erasmuswithoutpaper.registry.validators.coursesreplicationvalidator;

import eu.erasmuswithoutpaper.registry.validators.ApiEndpoint;
import eu.erasmuswithoutpaper.registry.validators.ValidatedApiInfo;

class CourseReplicationValidatedApiInfo extends ValidatedApiInfo {
  private final int version;
  private final ApiEndpoint endpoint;

  public CourseReplicationValidatedApiInfo(int version, ApiEndpoint endpoint) {
    this.version = version;
    this.endpoint = endpoint;
  }

  @Override
  public int getVersion() {
    return this.version;
  }

  @Override
  public String getPreferredPrefix() {
    return "cr";
  }

  @Override
  public boolean responseIncludeInCatalogueXmlns() {
    return false;
  }

  @Override
  public boolean apiEntryIncludeInCatalogueXmlns() {
    return false;
  }

  @Override
  public String getElementName() {
    return "course-replication-response";
  }

  @Override
  public String getApiName() {
    return "simple-course-replication";
  }

  @Override
  public String getGitHubRepositoryName() {
    return "course-replication";
  }

  @Override
  public String getNamespaceApiName() {
    return "api-course-replication";
  }

  @Override
  public ApiEndpoint getEndpoint() {
    return this.endpoint;
  }
}
