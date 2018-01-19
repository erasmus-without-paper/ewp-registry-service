package eu.erasmuswithoutpaper.registry.internet.sec;

import java.util.List;
import java.util.stream.Collectors;

import eu.erasmuswithoutpaper.registry.internet.Request;
import eu.erasmuswithoutpaper.registry.internet.Response;

/**
 * This {@link ResponseSigner} tries to sign the response using a couple of other
 * {@link ResponseSigner} implementations. It chooses the first implementation from the chain that
 * matches the client's request.
 */
public class ChainingResponseSigner implements ResponseSigner {

  private final List<ResponseSigner> signers;

  /**
   * @param signers The list {@link ResponseSigner}s to try signing with. We will call
   *        {@link ResponseSigner#wasRequestedFor(Request)} for each of those, and use the first one
   *        that returns true. If none of these will match the client's request, then
   *        {@link #sign(Request, Response)} will throw {@link Http4xx}.
   */
  public ChainingResponseSigner(List<ResponseSigner> signers) {
    this.signers = signers;
  }

  @Override
  public void sign(Request request, Response response) throws Http4xx {
    for (ResponseSigner signer : this.signers) {
      if (signer.wasRequestedFor(request)) {
        signer.sign(request, response);
        // First signer "wins".
        return;
      }
    }
    throw new Http4xx(400, "This endpoint requires the client to explicitly request one of the "
        + "following ways of response signing: "
        + this.signers.stream().map(signer -> signer.toString()).collect(Collectors.joining(", ")));
  }

  @Override
  public boolean wasRequestedFor(Request request) {
    for (ResponseSigner signer : this.signers) {
      if (signer.wasRequestedFor(request)) {
        return true;
      }
    }
    return false;
  }
}
