package eu.erasmuswithoutpaper.registry.echovalidator;

/**
 * Represents a single combination of all four types of v2 security methods, all of which are known
 * and can be validated by our validator.
 */
class SecMethodsCombination {

  private final SecMethod cliauth;
  private final SecMethod srvauth;
  private final SecMethod reqencr;

  private final SecMethod resencr;

  public SecMethodsCombination(SecMethod cliauth, SecMethod srvauth, SecMethod reqencr,
      SecMethod resencr) {
    this.cliauth = cliauth;
    this.srvauth = srvauth;
    this.reqencr = reqencr;
    this.resencr = resencr;
  }

  public SecMethod getCliAuth() {
    return this.cliauth;
  }

  public String getFourLetterCode() {
    return this.cliauth.toString() + this.srvauth.toString() + this.reqencr.toString()
        + this.resencr.toString();
  }

  public SecMethod getReqEncr() {
    return this.reqencr;
  }

  public SecMethod getResEncr() {
    return this.resencr;
  }

  public SecMethod getSrvAuth() {
    return this.srvauth;
  }

  @Override
  public String toString() {
    return "SecMethodCombination[" + this.getFourLetterCode() + "]";
  }
}
