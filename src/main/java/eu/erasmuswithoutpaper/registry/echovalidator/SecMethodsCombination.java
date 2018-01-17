package eu.erasmuswithoutpaper.registry.echovalidator;

import java.util.LinkedHashMap;

/**
 * Represents a single combination of all four types of v2 security methods, all of which are known
 * and can be validated by our validator.
 */
public class SecMethodsCombination {

  /**
   * @return An ordered map of "SecMethodCombination" codes mapped to their short names.
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
  private final SecMethod cliauth;
  private final SecMethod srvauth;
  private final SecMethod reqencr;
  private final SecMethod resencr;

  SecMethodsCombination(String httpmethod, SecMethod cliauth, SecMethod srvauth, SecMethod reqencr,
      SecMethod resencr) {
    this.httpmethod = httpmethod;
    this.cliauth = cliauth;
    this.srvauth = srvauth;
    this.reqencr = reqencr;
    this.resencr = resencr;
  }

  @Override
  public String toString() {
    return "SecMethodCombination[" + this.getFiveLetterCode() + "]";
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

  SecMethod getCliAuth() {
    return this.cliauth;
  }

  String getFiveLetterCode() {
    return this.getHttpMethodCode() + this.cliauth.toString() + this.srvauth.toString()
        + this.reqencr.toString() + this.resencr.toString();
  }

  String getHttpMethod() {
    return this.httpmethod;
  }

  SecMethod getReqEncr() {
    return this.reqencr;
  }

  SecMethod getResEncr() {
    return this.resencr;
  }

  SecMethod getSrvAuth() {
    return this.srvauth;
  }


  SecMethodsCombination withChangedCliAuth(SecMethod cliAuthMethod) {
    return new SecMethodsCombination(this.getHttpMethod(), cliAuthMethod, this.getSrvAuth(),
        this.getReqEncr(), this.getResEncr());
  }

  SecMethodsCombination withChangedHttpMethod(String httpMethod) {
    return new SecMethodsCombination(httpMethod, this.getCliAuth(), this.getSrvAuth(),
        this.getReqEncr(), this.getResEncr());
  }

  SecMethodsCombination withChangedReqEncr(SecMethod reqEncrMethod) {
    return new SecMethodsCombination(this.getHttpMethod(), this.getCliAuth(), this.getSrvAuth(),
        reqEncrMethod, this.getResEncr());
  }

  SecMethodsCombination withChangedResEncr(SecMethod resEncrMethod) {
    return new SecMethodsCombination(this.getHttpMethod(), this.getCliAuth(), this.getSrvAuth(),
        this.getReqEncr(), resEncrMethod);
  }

  SecMethodsCombination withChangedSrvAuth(SecMethod srvAuthMethod) {
    return new SecMethodsCombination(this.getHttpMethod(), this.getCliAuth(), srvAuthMethod,
        this.getReqEncr(), this.getResEncr());
  }
}
