package eu.erasmuswithoutpaper.registry.validators.coursesreplicationvalidator;

import eu.erasmuswithoutpaper.registry.documentbuilder.KnownElement;
import eu.erasmuswithoutpaper.registry.documentbuilder.KnownNamespace;
import eu.erasmuswithoutpaper.registry.validators.ApiEndpoint;
import eu.erasmuswithoutpaper.registry.validators.ValidatedApiInfo;

class CourseReplicationValidatedApiInfoV1 implements ValidatedApiInfo {
  @Override
  public KnownElement getResponseKnownElement() {
    return KnownElement.RESPONSE_COURSE_REPLICATION_V1;
  }

  @Override
  public KnownNamespace getApiEntryKnownNamespace() {
    return KnownNamespace.APIENTRY_COURSE_REPLICATION_V1;
  }

  @Override
  public String getApiName() {
    return "simple-course-replication";
  }

  @Override
  public ApiEndpoint getEndpoint() {
    return ApiEndpoint.NoEndpoint;
  }
}
