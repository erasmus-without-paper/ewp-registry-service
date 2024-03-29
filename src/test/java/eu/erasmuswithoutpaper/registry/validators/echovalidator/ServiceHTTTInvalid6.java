package eu.erasmuswithoutpaper.registry.validators.echovalidator;

import java.util.Collections;

import eu.erasmuswithoutpaper.registry.internet.sec.EwpHttpSigRequestAuthorizer;
import eu.erasmuswithoutpaper.registry.internet.sec.Http4xx;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

import net.adamcin.httpsig.api.Algorithm;
import net.adamcin.httpsig.api.Challenge;

/**
 * This one is a bit invalid, because it does include invalid headers in its WWW-Authenticate
 * header's `headers` key.
 */
public class ServiceHTTTInvalid6 extends ServiceHTTTValid {

  public ServiceHTTTInvalid6(String url, RegistryClient registryClient) {
    super(url, registryClient);
  }

  @Override
  protected EwpHttpSigRequestAuthorizer newAuthorizer() {
    return new EwpHttpSigRequestAuthorizer(this.registryClient) {
      @Override
      protected Http4xx newHttpSig401() {
        Http4xx e = super.newHttpSig401();
        Challenge newOne = new Challenge("EWP", Collections.singletonList("some-header"),
            Collections.singletonList(Algorithm.RSA_SHA256));
        e.putEwpErrorResponseHeader("WWW-Authenticate", newOne.getHeaderValue());
        return e;
      }
    };
  }
}
