package eu.erasmuswithoutpaper.registry.validators;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class ApiValidatorsManager {
  private static class ApiNameAndEndpoint {
    public final String apiName;
    public final ApiEndpoint endpoint;

    private ApiNameAndEndpoint(String apiName, ApiEndpoint endpoint) {
      this.apiName = apiName;
      this.endpoint = endpoint;
    }

    @Override
    public boolean equals(Object other) {
      if (this == other) {
        return true;
      }
      if (other == null || getClass() != other.getClass()) {
        return false;
      }
      ApiNameAndEndpoint that = (ApiNameAndEndpoint) other;
      return Objects.equals(apiName, that.apiName)
          && Objects.equals(endpoint, that.endpoint);
    }

    @Override
    public int hashCode() {
      return Objects.hash(apiName, endpoint);
    }
  }

  private static final Logger logger = LoggerFactory.getLogger(ApiValidatorsManager.class);
  private final Map<ApiNameAndEndpoint, ApiValidator<?>> registeredApiValidators = new HashMap<>();

  void registerApiValidator(String apiName, ApiEndpoint endpoint, ApiValidator<?> validator) {
    ApiNameAndEndpoint key = new ApiNameAndEndpoint(apiName, endpoint);

    if (registeredApiValidators.containsKey(key)) {
      logger.warn("ApiValidator for \""
          + apiName
          + (endpoint == null ? "" : ":" + endpoint)
          + "\" overridden.");
    }
    registeredApiValidators.put(new ApiNameAndEndpoint(apiName, endpoint), validator);
  }

  /**
   * Returns validator for provided api and endpoint.
   *
   * @param apiName
   *     name of api.
   * @param endpoint
   *     name of api's endpoint.
   * @return ApiValidator for api if it exists, null otherwise.
   */
  public ApiValidator<?> getApiValidator(String apiName, ApiEndpoint endpoint) {
    return registeredApiValidators.get(new ApiNameAndEndpoint(apiName, endpoint));
  }

  /**
   * Check if there are tests that can be run on certain version of certain api.
   *
   * @param apiName
   *     name of api to test.
   * @param version
   *     version of api to test.
   * @return true if there are any compatible tests.
   */
  public boolean hasCompatibleTests(String apiName, ApiEndpoint endpoint, SemanticVersion version) {
    ApiValidator<?> validator = getApiValidator(apiName, endpoint);
    if (validator == null) {
      return false;
    }

    for (SemanticVersion ver : validator.getCoveredApiVersions()) {
      if (version.isCompatible(ver)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Get parameters that can be passed when validating API `apiName` in version `version`.
   *
   * @param apiName
   *     API which parameters will be returned.
   * @param endpoint
   *     endpoint of the API.
   * @param version
   *     version of the API for which parameters will be returned.
   * @return List of parameters that can be used when validating API `apiName` in version `version`.
   */
  public List<ValidationParameter> getParameters(String apiName, ApiEndpoint endpoint,
      SemanticVersion version) {
    ApiValidator<?> validator = getApiValidator(apiName, endpoint);
    if (validator == null) {
      return new ArrayList<>();
    }
    return validator.getParameters(version);
  }
}
