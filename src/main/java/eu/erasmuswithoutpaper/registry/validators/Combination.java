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
   * Creates Combination from HTTP Method, url and entries of security description.
   * @param httpmethod HTTP Method used to perform request.
   * @param url URL to perform request to.
   * @param apiEntry Manifest API Entry related to url.
   * @param cliauth Client authentication method.
   * @param srvauth Server authentication method.
   * @param reqencr Request encryption method.
   * @param resencr Response encryption method.
   */
  public Combination(String httpmethod, String url, Element apiEntry, CombEntry cliauth,
      CombEntry srvauth, CombEntry reqencr, CombEntry resencr) {
    this.httpmethod = httpmethod;
    this.url = url;
    this.apiEntry = apiEntry;
    this.securityDescription = new HttpSecurityDescription(cliauth, srvauth, reqencr, resencr);
  }

  /**
   * Creates Combination from HTTP Method, url and security description.
   * @param httpmethod HTTP Method used to perform request.
   * @param url URL to perform request to.
   * @param apiEntry Manifest API Entry related to url.
   * @param securityDescription Security methods to be used.
   */
  public Combination(String httpmethod, String url, Element apiEntry,
      HttpSecurityDescription securityDescription) {
    this.httpmethod = httpmethod;
    this.url = url;
    this.apiEntry = apiEntry;
    this.securityDescription = securityDescription;
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

  /**
   * Obtain copy of this object with changed client authentication method.
   *
   * @param cliAuthMethod client authentication method
   *
   * @return Combination with changed parameter.
   */
  public Combination withChangedCliAuth(CombEntry cliAuthMethod) {
    return new Combination(this.getHttpMethod(), this.getUrl(), this.getApiEntry(), cliAuthMethod,
        this.getSrvAuth(), this.getReqEncr(), this.getResEncr());
  }

  /**
   * Obtain copy of this object with changed http method.
   *
   * @param httpMethod http method
   * @return Combination with changed parameter.
   */
  public Combination withChangedHttpMethod(String httpMethod) {
    return new Combination(httpMethod, this.getUrl(), this.getApiEntry(), this.getCliAuth(),
        this.getSrvAuth(), this.getReqEncr(), this.getResEncr());
  }

  /**
   * Obtain copy of this object with changed request encryption method.
   *
   * @param reqEncrMethod response encryption method.
   * @return Combination with changed parameter.
   */
  public Combination withChangedReqEncr(CombEntry reqEncrMethod) {
    return new Combination(this.getHttpMethod(), this.getUrl(), this.getApiEntry(),
        this.getCliAuth(), this.getSrvAuth(), reqEncrMethod, this.getResEncr());
  }

  /**
   * Obtain copy of this object with changed response encryption method.
   *
   * @param resEncrMethod response encryption method.
   * @return Combination with changed parameter.
   */
  public Combination withChangedResEncr(CombEntry resEncrMethod) {
    return new Combination(this.getHttpMethod(), this.getUrl(), this.getApiEntry(),
        this.getCliAuth(), this.getSrvAuth(), this.getReqEncr(), resEncrMethod);
  }

  /**
   * Obtain copy of this object with changed server authentication method.
   *
   * @param srvAuthMethod server authentication method.
   * @return Combination with changed parameter.
   */
  public Combination withChangedSrvAuth(CombEntry srvAuthMethod) {
    return new Combination(this.getHttpMethod(), this.getUrl(), this.getApiEntry(),
        this.getCliAuth(), srvAuthMethod, this.getReqEncr(), this.getResEncr());
  }

  /**
   * Obtain copy of this object with changed url.
   *
   * @param url url to use.
   * @return Combination with changed parameter.
   */
  public Combination withChangedUrl(String url) {
    return new Combination(this.getHttpMethod(), url, this.getApiEntry(), this.getCliAuth(),
        this.getSrvAuth(), this.getReqEncr(), this.getResEncr());
  }
}
