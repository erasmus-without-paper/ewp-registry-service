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
  public static LinkedHashMap<String, String> getCombinationLegend() {
    LinkedHashMap<String, String> options = new LinkedHashMap<>();
    options.put("G----", "HTTP GET method");
    options.put("P----", "HTTP POST method");
    options.put("-A---", "No Client Authentication (Anonymous Client)");
    options.put("-S---", "Client Authentication with TLS Certificate (self-signed)");
    options.put("-T---", "Client Authentication with TLS Certificate (CA-signed)");
    options.put("-H---", "Client Authentication with HTTP Signature");
    options.put("--T--", "Server Authentication with TLS Certificate (CA-signed)");
    options.put("--H--", "Server Authentication with HTTP Signature");
    options.put("---T-", "Request Encryption only with regular TLS");
    options.put("---E-", "Request Encryption with ewp-rsa-aes128gcm");
    options.put("----T", "Response Encryption only with regular TLS");
    options.put("----E", "Response Encryption with ewp-rsa-aes128gcm");
    return options;
  }

  private final String httpmethod;
  private final String url;
  private final Element apiEntry;

  private final CombEntry cliauth;
  private final CombEntry srvauth;
  private final CombEntry reqencr;
  private final CombEntry resencr;

  /**
   * Describes HTTP method, security methods used to authenticate user and server and
   * encryption methods.
   */
  public Combination(String httpmethod, String url, Element apiEntry, CombEntry cliauth,
                     CombEntry srvauth, CombEntry reqencr, CombEntry resencr) {
    this.httpmethod = httpmethod;
    this.url = url;
    this.apiEntry = apiEntry;
    this.cliauth = cliauth;
    this.srvauth = srvauth;
    this.reqencr = reqencr;
    this.resencr = resencr;
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
    return this.cliauth;
  }

  public String getFiveLetterCode() {
    return this.getHttpMethodCode() + this.cliauth.toString() + this.srvauth.toString()
        + this.reqencr.toString() + this.resencr.toString();
  }

  public String getHttpMethod() {
    return this.httpmethod;
  }

  public CombEntry getReqEncr() {
    return this.reqencr;
  }

  public CombEntry getResEncr() {
    return this.resencr;
  }

  public CombEntry getSrvAuth() {
    return this.srvauth;
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
