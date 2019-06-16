package eu.erasmuswithoutpaper.registry.validators.coursesreplicationvalidator;

import eu.erasmuswithoutpaper.registry.documentbuilder.KnownElement;
import eu.erasmuswithoutpaper.registry.documentbuilder.KnownNamespace;
import eu.erasmuswithoutpaper.registry.validators.ValidatedApiInfo;

class CourseReplicationValidatedApiInfoV1 implements ValidatedApiInfo {
  @Override
  public KnownElement getKnownElement() {
    return KnownElement.RESPONSE_COURSE_REPLICATION_V1;
  }

  @Override
  public String getApiNamespace() {
    return KnownNamespace.APIENTRY_COURSE_REPLICATION_V1.getNamespaceUri();
  }

  @Override
  public String getApiName() {
    return "simple-course-replication";
  }

  @Override
  public String getApiPrefix() {
    return "cr1";
  }

  @Override
  public String getApiResponsePrefix() {
    return "crr1";
  }

  @Override
  public String getEndpoint() {
    return NO_ENDPOINT;
  }
}
