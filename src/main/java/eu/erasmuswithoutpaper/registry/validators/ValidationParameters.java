package eu.erasmuswithoutpaper.registry.validators;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ValidationParameters {
  private Map<String, String> map = new HashMap<>();

  /**
   * Creates empty ValidationParameters.
   */
  public ValidationParameters() {

  }

  /**
   * Creates ValidationParameters object with values taken from input list.
   * @param values list of values to be stored in this object.
   */
  public ValidationParameters(List<ValidationParameterValue> values) {
    for (ValidationParameterValue parameterValue : values) {
      map.put(parameterValue.getName(), parameterValue.getValue());
    }
  }

  public String get(String parameterName) {
    return map.get(parameterName);
  }

  public boolean contains(String parameterName) {
    return map.containsKey(parameterName);
  }

  public void put(String parameter, String value) {
    map.put(parameter, value);
  }

  /**
   * Checks if parameters in this object fulfill dependencies of parameters.
   * @param parameters
   *    List of parameters with dependencies to check.
   * @return
   *    Whether parameters stored in this object fulfill dependencies of parameters.
   */
  public boolean checkDependencies(List<ValidationParameter> parameters) {
    for (ValidationParameter parameter : parameters) {
      if (!this.contains(parameter.getName())) {
        continue;
      }
      for (String dependency : parameter.getDependencies()) {
        if (!this.contains(dependency)) {
          return false;
        }
      }
      for (String blocker : parameter.getBlockers()) {
        if (this.contains(blocker)) {
          return false;
        }
      }
    }
    return true;
  }
}
