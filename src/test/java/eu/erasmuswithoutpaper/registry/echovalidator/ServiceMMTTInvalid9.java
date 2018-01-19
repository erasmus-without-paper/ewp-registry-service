package eu.erasmuswithoutpaper.registry.echovalidator;

import java.security.KeyPair;

import eu.erasmuswithoutpaper.registry.internet.Request;
import eu.erasmuswithoutpaper.registry.internet.Response;
import eu.erasmuswithoutpaper.registry.internet.sec.EwpHttpSigResponseSigner;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

/**
 * This one is invalid, because it always includes the X-Request-Id header, even when there was no
 * corresponding header in the request.
 */
public class ServiceMMTTInvalid9 extends ServiceMMTTValid {

  public ServiceMMTTInvalid9(String url, RegistryClient registryClient, KeyPair myKeyPair) {
    super(url, registryClient, myKeyPair);
  }

  @Override
  protected EwpHttpSigResponseSigner getHttpSigSigner() {
    return new EwpHttpSigResponseSigner(this.myKeyPair) {
      @Override
      protected void includeXRequestIdHeader(Request request, Response response) {
        super.includeXRequestIdHeader(request, response);
        if (response.getHeader("X-Request-Id") == null) {
          response.putHeader("X-Request-Id", ""); // empty, but exists.
        }
      }
    };
  }
}
