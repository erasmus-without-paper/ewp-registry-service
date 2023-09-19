package eu.erasmuswithoutpaper.registry.internet.sec;

/**
 * Special subclass of Http4xx. When thrown by {@link RequestAuthorizer}, it indicates that the
 * request is not using any of the authorization methods recognized by this
 * {@link RequestAuthorizer}.
 */
public class UnmatchedRequestAuthorizationMethod extends Http4xx {
  private static final long serialVersionUID = -6996262125623733090L;

  /**
   * @param statusCode HTTP status code to be returned to the client.
   * @param developerMessage The message describing the error which the client seems to have made.
   *        The message should state what kind of authorization is required by the
   *        {@link RequestAuthorizer}.
   */
  public UnmatchedRequestAuthorizationMethod(int statusCode, String developerMessage) {
    super(statusCode, developerMessage);
  }

}
