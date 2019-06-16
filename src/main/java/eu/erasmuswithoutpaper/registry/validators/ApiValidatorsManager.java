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
    public final String endpointName;

    private ApiNameAndEndpoint(String apiName, String endpointName) {
      this.apiName = apiName;
      this.endpointName = endpointName;
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
          && Objects.equals(endpointName, that.endpointName);
    }

    @Override
    public int hashCode() {
      return Objects.hash(apiName, endpointName);
    }
  }

  private static final Logger logger = LoggerFactory.getLogger(ApiValidatorsManager.class);
  private final Map<ApiNameAndEndpoint, ApiValidator<?>> registeredApiValidators = new HashMap<>();

  void registerApiValidator(String apiName, String endpointName, ApiValidator<?> validator) {
    ApiNameAndEndpoint key = new ApiNameAndEndpoint(apiName, endpointName);

    if (registeredApiValidators.containsKey(key)) {
      logger.warn("ApiValidator for \""
          + apiName
          + (endpointName == null ? "" : ":" + endpointName)
          + "\" overridden.");
    }
    registeredApiValidators.put(new ApiNameAndEndpoint(apiName, endpointName), validator);
  }

  /**
   * Returns validator for provided api and endpoint.
   *
   * @param apiName
   *     name of api.
   * @param endpointName
   *     name of api's endpoint.
   * @return ApiValidator for api if it exists, null otherwise.
   */
  public ApiValidator<?> getApiValidator(String apiName, String endpointName) {
    return registeredApiValidators.get(new ApiNameAndEndpoint(apiName, endpointName));
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
  public boolean hasCompatibleTests(String apiName, String endpointName, SemanticVersion version) {
    ApiValidator<?> validator = getApiValidator(apiName, endpointName);
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
   * @param endpointName
   *     endpoint of the API.
   * @param version
   *     version of the API for which parameters will be returned.
   * @return List of parameters that can be used when validating API `apiName` in version `version`.
   */
  public List<ValidationParameter> getParameters(String apiName, String endpointName,
      SemanticVersion version) {
    ApiValidator<?> validator = getApiValidator(apiName, endpointName);
    if (validator == null) {
      return new ArrayList<>();
    }
    return validator.getParameters(version);
  }
}
