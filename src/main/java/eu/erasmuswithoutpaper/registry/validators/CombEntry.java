package eu.erasmuswithoutpaper.registry.validators;

public enum CombEntry {

  CLIAUTH_NONE('A'), CLIAUTH_TLSCERT_SELFSIGNED('S'), CLIAUTH_HTTPSIG('H'),

  SRVAUTH_TLSCERT('T'), SRVAUTH_HTTPSIG('H'),

  REQENCR_TLS('T'), REQENCR_EWP('E'),

  RESENCR_TLS('T'), RESENCR_EWP('E');

  private final char code;

  CombEntry(char code) {
    this.code = code;
  }

  public char getCode() {
    return code;
  }

  @Override
  public String toString() {
    return String.valueOf(this.code);
  }
}
