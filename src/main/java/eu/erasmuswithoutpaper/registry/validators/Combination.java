package eu.erasmuswithoutpaper.registry.validators;

import java.util.LinkedHashMap;

import org.w3c.dom.Element;

/**
 * Represents a single combination of different methods in which a single endpoints can be tested.
 * These methods are generally non-overlapping, so putting them in such combinations seems a good
 * human-readable approach to be used by the validator.
 */
public class Combination {

  /**
   * @return An ordered map of {@link CombEntry} codes mapped to their short names.
   */
  private static LinkedHashMap<String, String> combinationLegend = new LinkedHashMap<>();

  static {
    LinkedHashMap<String, String> options = new LinkedHashMap<>();
    options.put("G----", "HTTP GET method");
    options.put("P----", "HTTP POST method");

    HttpSecurityDescription.getLegend().forEach((key, value) -> options.put("-" + key, value));
  }

  private final String httpmethod;
  private final String url;
  private final Element apiEntry;
  private final HttpSecurityDescription securityDescription;

  /**
   * Describes HTTP method, security methods used to authenticate user and server and encryption
   * methods.
   */
  public Combination(String httpmethod, String url, Element apiEntry, CombEntry cliauth,
      CombEntry srvauth, CombEntry reqencr, CombEntry resencr) {
    this.httpmethod = httpmethod;
    this.url = url;
    this.apiEntry = apiEntry;
    this.securityDescription = new HttpSecurityDescription(cliauth, srvauth, reqencr, resencr);
  }

  public static LinkedHashMap<String, String> getCombinationLegend() {
    return combinationLegend;
  }

  public HttpSecurityDescription getSecurityDescription() {
    return securityDescription;
  }

  @Override
  public String toString() {
    return "Combination[" + this.getFiveLetterCode() + "]";
  }

  private String getHttpMethodCode() {
    switch (this.getHttpMethod()) {
      case "POST":
        return "P";
      case "GET":
        return "G";
      default:
        return "-";
    }
  }

  public Element getApiEntry() {
    return this.apiEntry;
  }

  public CombEntry getCliAuth() {
    return this.securityDescription.getCliAuth();
  }

  public String getFiveLetterCode() {
    return this.getHttpMethodCode() + this.securityDescription.toString();
  }

  public String getHttpMethod() {
    return this.httpmethod;
  }

  public CombEntry getReqEncr() {
    return this.securityDescription.getReqEncr();
  }

  public CombEntry getResEncr() {
    return this.securityDescription.getResEncr();
  }

  public CombEntry getSrvAuth() {
    return this.securityDescription.getSrvAuth();
  }

  public String getUrl() {
    return this.url;
  }

  public Combination withChangedCliAuth(CombEntry cliAuthMethod) {
    return new Combination(this.getHttpMethod(), this.getUrl(), this.getApiEntry(), cliAuthMethod,
        this.getSrvAuth(), this.getReqEncr(), this.getResEncr());
  }

  public Combination withChangedHttpMethod(String httpMethod) {
    return new Combination(httpMethod, this.getUrl(), this.getApiEntry(), this.getCliAuth(),
        this.getSrvAuth(), this.getReqEncr(), this.getResEncr());
  }

  public Combination withChangedReqEncr(CombEntry reqEncrMethod) {
    return new Combination(this.getHttpMethod(), this.getUrl(), this.getApiEntry(),
        this.getCliAuth(), this.getSrvAuth(), reqEncrMethod, this.getResEncr());
  }

  public Combination withChangedResEncr(CombEntry resEncrMethod) {
    return new Combination(this.getHttpMethod(), this.getUrl(), this.getApiEntry(),
        this.getCliAuth(), this.getSrvAuth(), this.getReqEncr(), resEncrMethod);
  }

  public Combination withChangedSrvAuth(CombEntry srvAuthMethod) {
    return new Combination(this.getHttpMethod(), this.getUrl(), this.getApiEntry(),
        this.getCliAuth(), srvAuthMethod, this.getReqEncr(), this.getResEncr());
  }

  public Combination withChangedUrl(String url) {
    return new Combination(this.getHttpMethod(), url, this.getApiEntry(), this.getCliAuth(),
        this.getSrvAuth(), this.getReqEncr(), this.getResEncr());
  }

}
