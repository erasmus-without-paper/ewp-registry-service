package eu.erasmuswithoutpaper.registry.validators;

import java.util.LinkedHashMap;
import java.util.Objects;

/**
 * Describes HTTP method, security methods used to authenticate user and server and encryption
 * methods.
 */
public class HttpSecurityDescription {

  private static LinkedHashMap<String, String> legend = new LinkedHashMap<>();

  static {
    legend.put("A---", "No Client Authentication (Anonymous Client)");
    legend.put("S---", "Client Authentication with TLS Certificate (self-signed)");
    legend.put("T---", "Client Authentication with TLS Certificate (CA-signed)");
    legend.put("H---", "Client Authentication with HTTP Signature");
    legend.put("-T--", "Server Authentication with TLS Certificate (CA-signed)");
    legend.put("-H--", "Server Authentication with HTTP Signature");
    legend.put("--T-", "Request Encryption only with regular TLS");
    legend.put("--E-", "Request Encryption with ewp-rsa-aes128gcm");
    legend.put("---T", "Response Encryption only with regular TLS");
    legend.put("---E", "Response Encryption with ewp-rsa-aes128gcm");
  }

  private final CombEntry cliauth;
  private final CombEntry srvauth;
  private final CombEntry reqencr;
  private final CombEntry resencr;

  /**
   * Creates security description with given possibilities.
   */
  public HttpSecurityDescription(CombEntry cliauth, CombEntry srvauth, CombEntry reqencr,
      CombEntry resencr) {
    this.cliauth = cliauth;
    this.srvauth = srvauth;
    this.reqencr = reqencr;
    this.resencr = resencr;
  }

  /**
   * Creates security description from given string.
   *
   * @param description
   *     Correct security description of length 4.
   * @throws InvalidDescriptionString
   *     thrown if provided description string is not a correct description, i.e.: when its length
   *     isn't equal to 4 or when any of the characters is invalid or in incorrect place.
   */
  public HttpSecurityDescription(String description) throws InvalidDescriptionString {
    if (description == null || description.length() != 4) {
      throw new InvalidDescriptionString();
    }

    char cliAuthChar = description.charAt(0);
    if (cliAuthChar == CombEntry.CLIAUTH_NONE.getCode()) {
      cliauth = CombEntry.CLIAUTH_NONE;
    } else if (cliAuthChar == CombEntry.CLIAUTH_HTTPSIG.getCode()) {
      cliauth = CombEntry.CLIAUTH_HTTPSIG;
    } else if (cliAuthChar == CombEntry.CLIAUTH_TLSCERT_SELFSIGNED.getCode()) {
      cliauth = CombEntry.CLIAUTH_TLSCERT_SELFSIGNED;
    } else {
      throw new InvalidDescriptionString();
    }

    char srvAuthChar = description.charAt(1);
    if (srvAuthChar == CombEntry.SRVAUTH_TLSCERT.getCode()) {
      srvauth = CombEntry.SRVAUTH_TLSCERT;
    } else if (srvAuthChar == CombEntry.SRVAUTH_HTTPSIG.getCode()) {
      srvauth = CombEntry.SRVAUTH_HTTPSIG;
    } else {
      throw new InvalidDescriptionString();
    }

    char reqEncrChar = description.charAt(2);
    if (reqEncrChar == CombEntry.REQENCR_TLS.getCode()) {
      reqencr = CombEntry.REQENCR_TLS;
    } else if (reqEncrChar == CombEntry.REQENCR_EWP.getCode()) {
      reqencr = CombEntry.REQENCR_EWP;
    } else {
      throw new InvalidDescriptionString();
    }

    char resEncrChar = description.charAt(2);
    if (resEncrChar == CombEntry.RESENCR_TLS.getCode()) {
      resencr = CombEntry.RESENCR_TLS;
    } else if (resEncrChar == CombEntry.RESENCR_EWP.getCode()) {
      resencr = CombEntry.RESENCR_EWP;
    } else {
      throw new InvalidDescriptionString();
    }
  }

  /**
   * @return An ordered map of {@link CombEntry} codes mapped to their short names.
   */
  public static LinkedHashMap<String, String> getLegend() {
    return legend;
  }

  @Override
  public String toString() {
    return this.cliauth.toString() + this.srvauth.toString() + this.reqencr.toString()
        + this.resencr.toString();
  }

  public CombEntry getCliAuth() {
    return this.cliauth;
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

  public HttpSecurityDescription withChangedCliAuth(CombEntry cliAuthMethod) {
    return new HttpSecurityDescription(cliAuthMethod, this.getSrvAuth(), this.getReqEncr(),
        this.getResEncr());
  }

  public HttpSecurityDescription withChangedReqEncr(CombEntry reqEncrMethod) {
    return new HttpSecurityDescription(this.getCliAuth(), this.getSrvAuth(), reqEncrMethod,
        this.getResEncr());
  }

  public HttpSecurityDescription withChangedResEncr(CombEntry resEncrMethod) {
    return new HttpSecurityDescription(this.getCliAuth(), this.getSrvAuth(), this.getReqEncr(),
        resEncrMethod);
  }

  public HttpSecurityDescription withChangedSrvAuth(CombEntry srvAuthMethod) {
    return new HttpSecurityDescription(this.getCliAuth(), srvAuthMethod, this.getReqEncr(),
        this.getResEncr());
  }

  @Override
  public int hashCode() {
    return Objects.hash(cliauth, srvauth, reqencr, resencr);
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (other == null || getClass() != other.getClass()) {
      return false;
    }
    HttpSecurityDescription that = (HttpSecurityDescription) other;
    return cliauth == that.cliauth && srvauth == that.srvauth && reqencr == that.reqencr
        && resencr == that.resencr;
  }

  private String getLegendKey(char letter, int index) {
    StringBuilder sb = new StringBuilder("----");
    sb.setCharAt(index, letter);
    return sb.toString();
  }

  private String getExplanationPart(char letter, int index) {
    String key = getLegendKey(letter, index);
    String value = getLegend().get(key);
    return key + ":" + value;
  }

  /**
   * Creates human readable description of security methods described by this object.
   *
   * @return Four line string with description based on getLegend function.
   */
  public String getExplanation() {
    return getExplanationPart(cliauth.getCode(), 0)
        + "\n" + getExplanationPart(srvauth.getCode(), 1)
        + "\n" + getExplanationPart(reqencr.getCode(), 2)
        + "\n" + getExplanationPart(resencr.getCode(), 3);

  }

  public static class InvalidDescriptionString extends Exception {}
}
