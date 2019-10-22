package eu.erasmuswithoutpaper.registry.validators;

import java.util.LinkedHashMap;
import java.util.Objects;

import eu.erasmuswithoutpaper.registry.validators.echovalidator.HttpSecuritySettings;

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
   * @param cliauth Client authentication method.
   * @param srvauth Server authentication method.
   * @param reqencr Request encryption method.
   * @param resencr Response encryption method.
   */
  public HttpSecurityDescription(CombEntry cliauth, CombEntry srvauth, CombEntry reqencr,
      CombEntry resencr) {
    this.cliauth = cliauth;
    this.srvauth = srvauth;
    this.reqencr = reqencr;
    this.resencr = resencr;
  }

  /**
   * Maps given Client Authentication abbreviation to CombEntry.
   * @param code Client Authentication abbreviation letter.
   * @return Client Authentication CombEntry corresponding to given letter.
   * @throws InvalidDescriptionString if `code` was not a valid Client Authentication abbreviation.
   */
  public static CombEntry getClientAuthFromCode(char code) throws InvalidDescriptionString {
    if (code == CombEntry.CLIAUTH_NONE.getCode()) {
      return CombEntry.CLIAUTH_NONE;
    } else if (code == CombEntry.CLIAUTH_HTTPSIG.getCode()) {
      return CombEntry.CLIAUTH_HTTPSIG;
    } else if (code == CombEntry.CLIAUTH_TLSCERT_SELFSIGNED.getCode()) {
      return CombEntry.CLIAUTH_TLSCERT_SELFSIGNED;
    } else {
      throw new InvalidDescriptionString();
    }
  }

  /**
   * Maps given Server Authentication abbreviation to CombEntry.
   * @param code Server Authentication abbreviation letter.
   * @return Server Authentication CombEntry corresponding to given letter.
   * @throws InvalidDescriptionString if `code` was not a valid Server Authentication abbreviation.
   */
  public static CombEntry getServerAuthFromCode(char code) throws InvalidDescriptionString {
    if (code == CombEntry.SRVAUTH_TLSCERT.getCode()) {
      return CombEntry.SRVAUTH_TLSCERT;
    } else if (code == CombEntry.SRVAUTH_HTTPSIG.getCode()) {
      return CombEntry.SRVAUTH_HTTPSIG;
    } else {
      throw new InvalidDescriptionString();
    }
  }

  /**
   * Maps given Request Encryption abbreviation to CombEntry.
   * @param code Request Encryption abbreviation letter.
   * @return Request Encryption CombEntry corresponding to given letter.
   * @throws InvalidDescriptionString if `code` was not a valid Request Encryption abbreviation.
   */
  public static CombEntry getRequestEncryptionFromCode(char code) throws InvalidDescriptionString {
    if (code == CombEntry.REQENCR_TLS.getCode()) {
      return CombEntry.REQENCR_TLS;
    } else if (code == CombEntry.REQENCR_EWP.getCode()) {
      return CombEntry.REQENCR_EWP;
    } else {
      throw new InvalidDescriptionString();
    }
  }

  /**
   * Maps given Response Encryption abbreviation to CombEntry.
   * @param code Response Encryption abbreviation letter.
   * @return Response Encryption CombEntry corresponding to given letter.
   * @throws InvalidDescriptionString if `code` was not a valid Response Encryption abbreviation.
   */
  public static CombEntry getResponseEncryptionFromCode(char code) throws InvalidDescriptionString {
    if (code == CombEntry.RESENCR_TLS.getCode()) {
      return CombEntry.RESENCR_TLS;
    } else if (code == CombEntry.RESENCR_EWP.getCode()) {
      return CombEntry.RESENCR_EWP;
    } else {
      throw new InvalidDescriptionString();
    }
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
    cliauth = getClientAuthFromCode(cliAuthChar);

    char srvAuthChar = description.charAt(1);
    srvauth = getServerAuthFromCode(srvAuthChar);

    char reqEncrChar = description.charAt(2);
    reqencr = getRequestEncryptionFromCode(reqEncrChar);

    char resEncrChar = description.charAt(3);
    resencr = getResponseEncryptionFromCode(resEncrChar);
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

  /**
   * Checks is this security description is compatible with security settings provided in parameter.
   * @param httpSecurity HttpSecuritySettings to be compared against.
   * @return True, if httpSecurity supports methods described by this object.
   */
  public boolean isCompatible(HttpSecuritySettings httpSecurity) {
    if (this.cliauth == CombEntry.CLIAUTH_NONE && !httpSecurity.supportsCliAuthNone()) {
      return false;
    }
    if (this.cliauth == CombEntry.CLIAUTH_TLSCERT_SELFSIGNED
        && !httpSecurity.supportsCliAuthTlsCertSelfSigned()) {
      return false;
    }
    if (this.cliauth == CombEntry.CLIAUTH_HTTPSIG && !httpSecurity.supportsCliAuthHttpSig()) {
      return false;
    }

    if (this.srvauth == CombEntry.SRVAUTH_TLSCERT && !httpSecurity.supportsSrvAuthTlsCert()) {
      return false;
    }
    if (this.srvauth == CombEntry.SRVAUTH_TLSCERT && !httpSecurity.supportsSrvAuthTlsCert()) {
      return false;
    }

    if (this.reqencr == CombEntry.REQENCR_EWP && !httpSecurity.supportsReqEncrEwp()) {
      return false;
    }
    if (this.reqencr == CombEntry.REQENCR_TLS && !httpSecurity.supportsReqEncrTls()) {
      return false;
    }

    if (this.resencr == CombEntry.RESENCR_EWP && !httpSecurity.supportsResEncrEwp()) {
      return false;
    }
    if (this.resencr == CombEntry.RESENCR_TLS && !httpSecurity.supportsResEncrTls()) {
      return false;
    }

    return true;
  }

  public static class InvalidDescriptionString extends Exception {}
}
