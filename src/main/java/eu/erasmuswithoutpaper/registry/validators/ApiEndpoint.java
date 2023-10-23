package eu.erasmuswithoutpaper.registry.validators;

import java.util.HashMap;
import java.util.Map;

public enum ApiEndpoint {
  NO_ENDPOINT(""),
  GET("get"),
  INDEX("index"),
  UPDATE("update"),
  ;

  private static Map<String, ApiEndpoint> namesToEndpoints = new HashMap<>();

  static {
    for (ApiEndpoint endpoint : ApiEndpoint.values()) {
      namesToEndpoints.put(endpoint.name, endpoint);
    }
  }

  /**
   * Provides a mapping from name of the endpoint to corresponding ApiEndpoint object.
   * @param name name of the endpoint.
   * @return corresponding ApiEndpoint if exists.
   * @throws IllegalArgumentException is corresponding ApiEndpoint doesn't exist.
   */
  public static ApiEndpoint fromEndpointName(String name) throws IllegalArgumentException {
    String noNullName = "";
    if (name != null) {
      noNullName = name;
    }
    ApiEndpoint result = namesToEndpoints.get(noNullName);
    if (result == null) {
      throw new IllegalArgumentException("No ApiEndpoint enum for \"" + name + "\"");
    }
    return result;
  }

  private String name;

  ApiEndpoint(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  @Override
  public String toString() {
    return this.name;
  }
}
