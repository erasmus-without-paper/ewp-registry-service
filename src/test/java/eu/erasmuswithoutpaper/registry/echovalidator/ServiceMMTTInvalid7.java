package eu.erasmuswithoutpaper.registry.echovalidator;

import java.security.KeyPair;

import eu.erasmuswithoutpaper.registry.internet.Request;
import eu.erasmuswithoutpaper.registry.internet.Response;
import eu.erasmuswithoutpaper.registry.internet.sec.EwpHttpSigResponseSigner;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

/**
 * This one is invalid, because it includes a bad X-Request-Signature header.
 */
public class ServiceMMTTInvalid7 extends ServiceMMTTValid {

  public ServiceMMTTInvalid7(String url, RegistryClient registryClient, String myKeyId,
      KeyPair myKeyPair) {
    super(url, registryClient, myKeyId, myKeyPair);
  }

  @Override
  protected EwpHttpSigResponseSigner getHttpSigSigner() {
    return new EwpHttpSigResponseSigner(this.myKeyId, this.myKeyPair) {
      @Override
      protected void includeXRequestSignature(Request request, Response response) {
        response.putHeader("X-Request-Signature", "Bad Signature");
      }
    };
  }
}
