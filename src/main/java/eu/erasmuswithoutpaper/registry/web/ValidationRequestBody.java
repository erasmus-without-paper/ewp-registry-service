package eu.erasmuswithoutpaper.registry.web;

import static eu.erasmuswithoutpaper.registry.validators.ValidatedApiInfo.NO_ENDPOINT;

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
   * @return endpoint if it is non-empty string, NO_ENDPOINT otherwise.
   */
  public String getEndpoint() {
    if (endpoint == null || endpoint.isEmpty()) {
      return NO_ENDPOINT;
    }
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
