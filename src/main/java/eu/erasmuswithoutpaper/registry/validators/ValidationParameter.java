package eu.erasmuswithoutpaper.registry.validators;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ValidationParameter {
  private String name;
  private List<String> dependencies = new ArrayList<>();
  private List<String> blockers = new ArrayList<>();
  private String description;

  public ValidationParameter(String name) {
    this.name = name;
  }

  public ValidationParameter(String name, List<String> dependencies) {
    this.name = name;
    this.dependencies = dependencies;
  }

  public ValidationParameter dependsOn(String... dependencies) {
    this.dependencies.addAll(Arrays.asList(dependencies));
    return this;
  }

  public ValidationParameter blockedBy(String... blockers) {
    this.blockers.addAll(Arrays.asList(blockers));
    return this;
  }

  public ValidationParameter withDescription(String description) {
    this.description = description;
    return this;
  }

  public String getName() {
    return name;
  }

  public List<String> getDependencies() {
    return dependencies;
  }

  public List<String> getBlockers() {
    return blockers;
  }

  public String getDescription() {
    return description;
  }
}
