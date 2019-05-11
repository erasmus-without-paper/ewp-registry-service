package eu.erasmuswithoutpaper.registry.validators;

import java.util.ArrayList;
import java.util.List;

public class ValidationParameter {
  private String name;
  private List<String> dependencies;

  public ValidationParameter(String name) {
    this.name = name;
    this.dependencies = new ArrayList<>();
  }

  public ValidationParameter(String name, List<String> dependencies) {
    this.name = name;
    this.dependencies = dependencies;
  }

  public String getName() {
    return name;
  }

  public List<String> getDependencies() {
    return dependencies;
  }
}
