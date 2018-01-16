package eu.erasmuswithoutpaper.registry.internet.sec;

import java.net.URL;

import eu.erasmuswithoutpaper.registry.internet.Request;
import eu.erasmuswithoutpaper.registry.internet.Response;

/**
 * This {@link ResponseAuthorizer} "authorizes" responses simply by making sure that TLS was used
 * during transport. It's present here primarily for completeness.
 */
public class TlsResponseAuthorizer extends CommonResponseAuthorizer {

  @Override
  public EwpServer authorize(Request request, Response response) throws CouldNotAuthorize {
    URL url = this.parseUrl(request);
    this.verifyProtocol(url);
    this.verifyRequestId(request, response);
    return new EwpServerWithCertificate(url.getHost());
  }
}
