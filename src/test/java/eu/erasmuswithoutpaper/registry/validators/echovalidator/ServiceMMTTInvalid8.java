package eu.erasmuswithoutpaper.registry.validators.echovalidator;

import java.security.KeyPair;
import java.util.List;

import eu.erasmuswithoutpaper.registry.internet.Request;
import eu.erasmuswithoutpaper.registry.internet.Response;
import eu.erasmuswithoutpaper.registry.internet.sec.EwpHttpSigResponseSigner;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

/**
 * This one is invalid, because it doesn't sign the digest header.
 */
public class ServiceMMTTInvalid8 extends ServiceMMTTValid {

  public ServiceMMTTInvalid8(String url, RegistryClient registryClient, KeyPair myKeyPair) {
    super(url, registryClient, myKeyPair);
  }

  @Override
  protected EwpHttpSigResponseSigner getHttpSigSigner() {
    return new EwpHttpSigResponseSigner(this.myKeyPair) {
      @Override
      protected List<String> getHeadersToSign(Request request, Response response) {
        List<String> result = super.getHeadersToSign(request, response);
        result.remove("Digest");
        return result;
      }
    };
  }
}
