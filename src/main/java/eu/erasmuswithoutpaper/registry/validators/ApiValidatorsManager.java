package eu.erasmuswithoutpaper.registry.validators;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class ApiValidatorsManager {
  private static final Logger logger = LoggerFactory.getLogger(ApiValidatorsManager.class);
  private final Map<String, ApiValidator<?>> registeredApiValidators = new HashMap<>();

  void registerApiValidator(String apiName, ApiValidator<?> validator) {
    if (registeredApiValidators.containsKey(apiName)) {
      logger.warn("ApiValidator for \"" + apiName + "\" overridden.");
    }
    registeredApiValidators.put(apiName, validator);
  }

  /**
   * Returns validator for provided api.
   *
   * @param apiName
   *     name of api.
   * @return ApiValidator for api if it exists, null otherwise.
   */
  public ApiValidator<?> getApiValidator(String apiName) {
    return registeredApiValidators.get(apiName);
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
  public boolean hasCompatibleTests(String apiName, SemanticVersion version) {
    if (!registeredApiValidators.containsKey(apiName)) {
      return false;
    }
    ApiValidator<?> validator = getApiValidator(apiName);
    for (SemanticVersion ver : validator.getCoveredApiVersions()) {
      if (version.isCompatible(ver)) {
        return true;
      }
    }
    return false;
  }
}
