package eu.erasmuswithoutpaper.registry.echovalidator;

/**
 * Represents a single combination of all four types of v2 security methods, all of which are known
 * and can be validated by our validator.
 */
class KnownHttpSecurityMethodsCombination {

  private final KnownMethodId cliauth;
  private final KnownMethodId srvauth;
  private final KnownMethodId reqencr;
  private final KnownMethodId resencr;

  public KnownHttpSecurityMethodsCombination(KnownMethodId cliauth, KnownMethodId srvauth,
      KnownMethodId reqencr, KnownMethodId resencr) {
    this.cliauth = cliauth;
    this.srvauth = srvauth;
    this.reqencr = reqencr;
    this.resencr = resencr;
  }

  public KnownMethodId getCliAuth() {
    return this.cliauth;
  }

  public String getFourLetterCode() {
    return this.cliauth.toString() + this.srvauth.toString() + this.reqencr.toString()
        + this.resencr.toString();
  }

  public KnownMethodId getReqEncr() {
    return this.reqencr;
  }

  public KnownMethodId getResEncr() {
    return this.resencr;
  }

  public KnownMethodId getSrvAuth() {
    return this.srvauth;
  }

  @Override
  public String toString() {
    /*
     * Note, that once the validator supports more combinations, we will probably need to return the
     * "four letter code" instead, because textual descriptions will simply get too long.
     */
    if (this.srvauth.equals(KnownMethodId.SRVAUTH_TLSCERT)
        && this.reqencr.equals(KnownMethodId.REQENCR_TLS)
        && this.resencr.equals(KnownMethodId.RESENCR_TLS)) {
      if (this.cliauth.equals(KnownMethodId.CLIAUTH_NONE)) {
        return "Anonymous Client";
      } else if (this.cliauth.equals(KnownMethodId.CLIAUTH_TLSCERT_SELFSIGNED)) {
        return "TLS Client Certificate Authentication";
      } else if (this.cliauth.equals(KnownMethodId.CLIAUTH_HTTPSIG)) {
        return "HTTPSIG Client Authentication";
      } else {
        throw new RuntimeException();
      }
    } else {
      return "SecurityCombination[" + this.getFourLetterCode() + "]";
    }
  }
}
