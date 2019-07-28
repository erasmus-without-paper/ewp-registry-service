package eu.erasmuswithoutpaper.registry.validators.coursesvalidator;

import eu.erasmuswithoutpaper.registry.documentbuilder.KnownElement;
import eu.erasmuswithoutpaper.registry.documentbuilder.KnownNamespace;
import eu.erasmuswithoutpaper.registry.validators.ApiEndpoint;
import eu.erasmuswithoutpaper.registry.validators.ValidatedApiInfo;

class CoursesValidatedApiInfoV070 implements ValidatedApiInfo {
  @Override
  public KnownElement getResponseKnownElement() {
    return KnownElement.RESPONSE_COURSES_V1;
  }

  @Override
  public KnownNamespace getApiEntryKnownNamespace() {
    return KnownNamespace.APIENTRY_COURSES_V1;
  }

  @Override
  public String getApiName() {
    return "courses";
  }

  public ApiEndpoint getEndpoint() {
    return ApiEndpoint.NoEndpoint;
  }
}
