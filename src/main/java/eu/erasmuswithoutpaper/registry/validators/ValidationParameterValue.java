package eu.erasmuswithoutpaper.registry.validators;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@SuppressFBWarnings("UWF_UNWRITTEN_FIELD")
public class ValidationParameterValue {
  private String name;
  private String value;

  public ValidationParameterValue() {
  }

  public ValidationParameterValue(String name, String value) {
    this.name = name;
    this.value = value;
  }

  public String getName() {
    return name;
  }

  public String getValue() {
    return value;
  }
}
