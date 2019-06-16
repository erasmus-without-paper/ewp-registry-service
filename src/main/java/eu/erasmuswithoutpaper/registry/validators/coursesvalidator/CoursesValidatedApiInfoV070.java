package eu.erasmuswithoutpaper.registry.validators.coursesvalidator;

import eu.erasmuswithoutpaper.registry.documentbuilder.KnownElement;
import eu.erasmuswithoutpaper.registry.documentbuilder.KnownNamespace;
import eu.erasmuswithoutpaper.registry.validators.ValidatedApiInfo;

class CoursesValidatedApiInfoV070 implements ValidatedApiInfo {
  @Override
  public KnownElement getKnownElement() {
    return KnownElement.RESPONSE_COURSES_V1;
  }

  @Override
  public String getApiNamespace() {
    return KnownNamespace.APIENTRY_COURSES_V1.getNamespaceUri();
  }

  @Override
  public String getApiName() {
    return "courses";
  }

  @Override
  public String getApiPrefix() {
    return "co1";
  }

  @Override
  public String getApiResponsePrefix() {
    return "cor1";
  }

  @Override
  public String getEndpoint() {
    return NO_ENDPOINT;
  }
}
