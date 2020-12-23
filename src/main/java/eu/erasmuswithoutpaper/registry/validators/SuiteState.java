package eu.erasmuswithoutpaper.registry.validators;

import java.util.ArrayList;
import java.util.List;

import eu.erasmuswithoutpaper.registryclient.ApiSearchConditions;

public class SuiteState {
  public final String url;
  public final SemanticVersion version;
  public boolean broken = false;
  public List<Combination> combinations = new ArrayList<>();

  public ValidationParameters parameters = new ValidationParameters();
  public ApiSearchConditions apiSearchConditions;

  public SuiteState(String url, SemanticVersion version) {
    this.url = url;
    this.version = version;
  }
}
