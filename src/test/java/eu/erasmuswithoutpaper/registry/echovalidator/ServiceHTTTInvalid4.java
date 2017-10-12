package eu.erasmuswithoutpaper.registry.echovalidator;

import java.util.List;

import eu.erasmuswithoutpaper.registry.internet.Internet.Request;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

import com.google.common.collect.Lists;
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
  protected Authorization verifyHttpSignatureAuthorizationHeader(Request request)
      throws ErrorResponseException {
    Authorization authz = super.verifyHttpSignatureAuthorizationHeader(request);
    List<String> expected = Lists.newArrayList("(request-target)", "host", "date", "original-date",
        "digest", "x-request-id");
    for (String headerName : authz.getHeaders()) {
      if (!expected.contains(headerName)) {
        throw new ErrorResponseException(this.createErrorResponse(request, 400,
            "How rude of you! You have signed a " + headerName + " header. I won't accept that."));
      }
    }
    return authz;
  }

}
