package eu.erasmuswithoutpaper.registry.echovalidator;

import java.security.KeyPair;

import eu.erasmuswithoutpaper.registry.internet.Response;
import eu.erasmuswithoutpaper.registry.internet.sec.EwpHttpSigResponseSigner;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

/**
 * This one includes "original-date" instead of "date". This is valid.
 */
public class ServiceMMTTValid2 extends ServiceMMTTValid {

  public ServiceMMTTValid2(String url, RegistryClient registryClient, String myKeyId,
      KeyPair myKeyPair) {
    super(url, registryClient, myKeyId, myKeyPair);
  }

  @Override
  protected EwpHttpSigResponseSigner getHttpSigSigner() {
    return new EwpHttpSigResponseSigner(this.myKeyId, this.myKeyPair) {
      @Override
      protected void includeDateHeaders(Response response) {
        this.includeDateHeaders(response, false, true);
      }
    };
  }
}
