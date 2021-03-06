package eu.erasmuswithoutpaper.registry.web;

import java.util.List;

import eu.erasmuswithoutpaper.registry.validators.ValidationParameterValue;

public class ValidationRequestBody {
  private String name;
  private String endpoint;
  private String security;
  private String url;
  private String version;
  private List<ValidationParameterValue> parameters;

  public String getName() {
    return name;
  }

  /**
   * Gets API endpoint received in the request.
   *
   * @return ApiEndpoint corresponding to this endpoint or null if it is unknown
   */
  public String getEndpoint() {
    return endpoint;
  }

  public String getSecurity() {
    return security;
  }

  public String getUrl() {
    return url;
  }

  public String getVersion() {
    return version;
  }

  public List<ValidationParameterValue> getParameters() {
    return parameters;
  }
}
