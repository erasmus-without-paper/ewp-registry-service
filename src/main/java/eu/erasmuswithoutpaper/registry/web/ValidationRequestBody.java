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
