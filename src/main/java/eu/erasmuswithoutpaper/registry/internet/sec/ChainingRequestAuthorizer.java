package eu.erasmuswithoutpaper.registry.internet.sec;

import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import eu.erasmuswithoutpaper.registry.internet.Request;

/**
 * This {@link RequestAuthorizer} tries several other {@link RequestAuthorizer}s in sequence, until
 * it finds one which succeeds.
 */
public class ChainingRequestAuthorizer implements RequestAuthorizer {

  /**
   * The list of authorizers to the to authorize requests with.
   */
  protected final List<RequestAuthorizer> authorizers;

  /**
   * The authorizer on which error response's to base our final error response, in case if none of
   * the authorizers succeed. This authorizer is also present on the {@link #authorizers} list.
   */
  protected final RequestAuthorizer preferredAuthorizer;

  /**
   * @param authorizers The list of authorized to try (in that order).
   * @param preferredAuthorizer The authorizer on which error response's to base our final error
   *        response, in case if none of the authorizers succeed (e.g. WWW-Authenticate header will
   *        be copied from its error response). This authorizer MUST be on the list of authorizer
   *        passed in the previous argument.
   */
  public ChainingRequestAuthorizer(List<RequestAuthorizer> authorizers,
      RequestAuthorizer preferredAuthorizer) {
    this.authorizers = authorizers;
    this.preferredAuthorizer = preferredAuthorizer;
    if (!this.authorizers.contains(this.preferredAuthorizer)) {
      throw new RuntimeException();
    }
  }

  @Override
  public EwpClient authorize(Request request) throws Http4xx, UnmatchedRequestAuthorizationMethod {
    for (Iterator<RequestAuthorizer> iterator = this.authorizers.iterator(); iterator.hasNext();) {
      RequestAuthorizer authorizer = iterator.next();
      try {
        return authorizer.authorize(request);
      } catch (UnmatchedRequestAuthorizationMethod e) {
        if (!iterator.hasNext()) {
          throw this.withChangedMessage(e, "Could not authorize this request. "
              + "Authorizers tried: "
              + this.authorizers.stream().map(s -> s.toString()).collect(Collectors.joining(", ")));
        }
      } catch (Http4xx e) {
        throw this.withChangedMessage(e, "While trying " + authorizer + ": " + e.getMessage());
      }
    }
    throw new RuntimeException(); // Shouldn't happen.
  }

  private Http4xx withChangedMessage(Http4xx prev, String newMessage) {
    Http4xx result = new Http4xx(prev.getStatusCode(), newMessage);
    for (Entry<String, String> entry : prev.getHeaders().entrySet()) {
      result.putEwpErrorResponseHeader(entry.getKey(), entry.getValue());
    }
    return result;
  }

  private UnmatchedRequestAuthorizationMethod withChangedMessage(
      UnmatchedRequestAuthorizationMethod prev, String newMessage) {
    UnmatchedRequestAuthorizationMethod result =
        new UnmatchedRequestAuthorizationMethod(prev.getStatusCode(), newMessage);
    for (Entry<String, String> entry : prev.getHeaders().entrySet()) {
      result.putEwpErrorResponseHeader(entry.getKey(), entry.getValue());
    }
    return result;
  }
}
