package eu.erasmuswithoutpaper.registry.internet.sec;

import eu.erasmuswithoutpaper.registry.internet.Request;

/**
 * This {@link RequestSigner} simply <b>doesn't</b> sign the requests. It's a "fake" signer.
 */
public class AnonymousRequestSigner implements RequestSigner {

  @Override
  public void sign(Request request) {
  }

}
