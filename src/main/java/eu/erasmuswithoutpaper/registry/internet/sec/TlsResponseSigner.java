package eu.erasmuswithoutpaper.registry.internet.sec;

import java.util.Locale;

import eu.erasmuswithoutpaper.registry.internet.Request;
import eu.erasmuswithoutpaper.registry.internet.Response;

/**
 * This {@link ResponseSigner} "signs" responses simply by making sure that TLS is used during
 * transport. It's present here primarily for completeness.
 */
public class TlsResponseSigner extends CommonResponseSigner {

  @Override
  public void sign(Request request, Response response) throws Http4xx {
    this.includeXRequestIdHeader(request, response);
    this.verifyScheme(request);
  }

  @Override
  public String toString() {
    return "Regular TLS Response Signer";
  }

  @Override
  public boolean wasRequestedFor(Request request) {
    /*
     * We can safely assume, that if the client is making the request over HTTPS, then he is
     * requesting the server to authenticate itself properly using TLS certificate.
     */
    return request.getUrl().toLowerCase(Locale.US).startsWith("https://");
  }

  /**
   * Verify if the response is being transmitted via TLS.
   *
   * @param request Actually we need the request object to verify that, not the response one.
   * @throws Http4xx If the request was made via unsecure protocol.
   */
  protected void verifyScheme(Request request) throws Http4xx {
    if (!request.getUrl().toLowerCase(Locale.US).startsWith("https://")) {
      throw new Http4xx(400, "Requests need to be made over TLS (https) connection.");
    }
  }
}
