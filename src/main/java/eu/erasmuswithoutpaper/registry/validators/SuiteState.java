package eu.erasmuswithoutpaper.registry.validators;

import java.util.ArrayList;
import java.util.List;

import eu.erasmuswithoutpaper.registry.internet.sec.EwpHttpSigResponseAuthorizer;

import org.w3c.dom.Element;

public class SuiteState {
  public boolean broken = false;
  public List<Combination> combinations = new ArrayList<>();
  public EwpHttpSigResponseAuthorizer resAuthorizerHttpSig;
  public Element matchedApiEntry;
}
