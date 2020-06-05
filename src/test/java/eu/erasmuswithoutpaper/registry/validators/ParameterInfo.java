package eu.erasmuswithoutpaper.registry.validators;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ParameterInfo {
  public static ParameterInfo readParam(Map<String, List<String>> params, String param) {
    List<String> allValues = params.getOrDefault(param, new ArrayList<>());
    return new ParameterInfo(
        allValues,
        allValues.size() > 0,
        allValues.size() > 1,
        allValues.isEmpty() ? null : allValues.get(0),
        allValues.isEmpty() ? 0 : 1
    );
  }

  private ParameterInfo(List<String> allValues, boolean hasAny, boolean hasMultiple,
      String firstValueOrNull, int coveredParameters) {
    this.hasAny = hasAny;
    this.hasMultiple = hasMultiple;
    this.allValues = allValues;
    this.firstValueOrNull = firstValueOrNull;
    this.coveredParameters = coveredParameters;
  }

  public String getValueOrDefault(String defaultValue) {
    if (firstValueOrNull == null) {
      return defaultValue;
    }
    return firstValueOrNull;
  }

  public final boolean hasAny;
  public final boolean hasMultiple;
  public final List<String> allValues;
  public final String firstValueOrNull;
  public final int coveredParameters;
}
