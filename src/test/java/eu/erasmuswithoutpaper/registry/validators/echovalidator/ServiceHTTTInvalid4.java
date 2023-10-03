package eu.erasmuswithoutpaper.registry.validators.echovalidator;

import java.util.Arrays;
import java.util.List;

import eu.erasmuswithoutpaper.registry.internet.Request;
import eu.erasmuswithoutpaper.registry.internet.sec.EwpHttpSigRequestAuthorizer;
import eu.erasmuswithoutpaper.registry.internet.sec.Http4xx;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

import net.adamcin.httpsig.api.Authorization;

/**
 * This one is invalid, because expects a very specific set of headers to be signed, and doesn't
 * allow any more signed headers than those.
 */
public class ServiceHTTTInvalid4 extends ServiceHTTTValid {

  public ServiceHTTTInvalid4(String url, RegistryClient registryClient) {
    super(url, registryClient);
  }

  @Override
  protected EwpHttpSigRequestAuthorizer newAuthorizer() {
    return new EwpHttpSigRequestAuthorizer(this.registryClient) {
      @Override
      protected Authorization verifyHttpSignatureAuthorizationHeader(Request request)
          throws Http4xx {
        Authorization authz = super.verifyHttpSignatureAuthorizationHeader(request);
        List<String> expected = Arrays.asList("(request-target)", "host", "date",
            "original-date", "digest", "x-request-id");
        for (String headerName : authz.getHeaders()) {
          if (!expected.contains(headerName)) {
            throw new Http4xx(400, "How rude of you! You have signed a " + headerName
                + " header. I won't accept that.");
          }
        }
        return authz;
      }
    };
  }

}
