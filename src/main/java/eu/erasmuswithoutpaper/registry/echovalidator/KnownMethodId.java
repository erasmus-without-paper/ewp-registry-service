package eu.erasmuswithoutpaper.registry.echovalidator;

enum KnownMethodId {
  CLIAUTH_NONE('A'), CLIAUTH_TLSCERT_SELFSIGNED('S'), CLIAUTH_HTTPSIG('H'),

  SRVAUTH_TLSCERT('T'), SRVAUTH_HTTPSIG('H'),

  REQENCR_TLS('T'),

  RESENCR_TLS('T');

  private final char code;

  private KnownMethodId(char code) {
    this.code = code;
  }

  @Override
  public String toString() {
    return String.valueOf(this.code);
  }
}
