package eu.erasmuswithoutpaper.registry.validators.coursesvalidator;

import eu.erasmuswithoutpaper.registry.validators.ApiEndpoint;
import eu.erasmuswithoutpaper.registry.validators.ValidatedApiInfo;

class CoursesValidatedApiInfo implements ValidatedApiInfo {
  private final int version;
  private final ApiEndpoint endpoint;

  public CoursesValidatedApiInfo(int version, ApiEndpoint endpoint) {
    this.version = version;
    this.endpoint = endpoint;
  }

  @Override
  public int getVersion() {
    return this.version;
  }

  @Override
  public String getPreferredPrefix() {
    return "co";
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
  public String getApiName() {
    return "courses";
  }

  @Override
  public ApiEndpoint getEndpoint() {
    return this.endpoint;
  }
}
