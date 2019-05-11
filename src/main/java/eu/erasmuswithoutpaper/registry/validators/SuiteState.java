package eu.erasmuswithoutpaper.registry.validators;

import java.util.ArrayList;
import java.util.List;

import eu.erasmuswithoutpaper.registry.internet.sec.EwpHttpSigResponseAuthorizer;

import org.w3c.dom.Element;

public class SuiteState {
  public final String url;
  public final SemanticVersion version;
  public boolean broken = false;
  public List<Combination> combinations = new ArrayList<>();
  public EwpHttpSigResponseAuthorizer resAuthorizerHttpSig;
  public Element matchedApiEntry;

  public ValidationParameters parameters = new ValidationParameters();

  public SuiteState(String url, SemanticVersion version) {
    this.url = url;
    this.version = version;
  }
}
