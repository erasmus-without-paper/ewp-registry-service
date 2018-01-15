package eu.erasmuswithoutpaper.registry.echovalidator;

import java.security.KeyPair;

import eu.erasmuswithoutpaper.registry.internet.Response;
import eu.erasmuswithoutpaper.registry.internet.sec.EwpHttpSigResponseSigner;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

/**
 * This one is invalid, because it doesn't include the Date nor Original-Date headers.
 */
public class ServiceMMTTInvalid1 extends ServiceMMTTValid {

  public ServiceMMTTInvalid1(String url, RegistryClient registryClient, String myKeyId,
      KeyPair myKeyPair) {
    super(url, registryClient, myKeyId, myKeyPair);
  }

  @Override
  protected EwpHttpSigResponseSigner getHttpSigSigner() {
    return new EwpHttpSigResponseSigner(this.myKeyId, this.myKeyPair) {
      @Override
      protected void includeDateHeaders(Response response) {
        // Do nothing.
      }
    };
  }
}
