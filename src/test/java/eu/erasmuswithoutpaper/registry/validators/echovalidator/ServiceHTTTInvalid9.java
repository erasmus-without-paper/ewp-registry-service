package eu.erasmuswithoutpaper.registry.validators.echovalidator;

import java.security.interfaces.RSAPublicKey;

import eu.erasmuswithoutpaper.registry.internet.Request;
import eu.erasmuswithoutpaper.registry.internet.sec.EwpHttpSigRequestAuthorizer;
import eu.erasmuswithoutpaper.registry.internet.sec.Http4xx;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

import net.adamcin.httpsig.api.Authorization;

/**
 * This one is invalid, because it doesn't verify the signature.
 */
public class ServiceHTTTInvalid9 extends ServiceHTTTValid {

  public ServiceHTTTInvalid9(String url, RegistryClient registryClient) {
    super(url, registryClient);
  }

  @Override
  protected EwpHttpSigRequestAuthorizer newAuthorizer() {
    return new EwpHttpSigRequestAuthorizer(this.registryClient) {
      @Override
      protected void verifySignature(Request request, RSAPublicKey expectedClientKey,
          Authorization authz) throws Http4xx {
        return;
      }
    };
  }
}
