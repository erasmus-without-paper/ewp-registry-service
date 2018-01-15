package eu.erasmuswithoutpaper.registry.internet.sec;

/**
 * Thrown by {@link ResponseAuthorizer}s when the response cannot be properly authorized (i.e. the
 * server's signature doesn't match what the authorizer expects).
 */
@SuppressWarnings("serial")
public class CouldNotAuthorize extends Exception {

  /**
   * @param message The message describing what went wrong during the authorization process.
   */
  public CouldNotAuthorize(String message) {
    super(message);
  }
}
