package eu.erasmuswithoutpaper.registry.internet;

import java.io.IOException;

/**
 * A virtual HTTP service that can be plugged into the {@link FakeInternet} instance (for testing
 * purposes).
 */
public interface FakeInternetService {

  /**
   * Handle a request.
   *
   * <p>
   * Note, that {@link FakeInternet} doesn't have a router - each {@link FakeInternetService}
   * decides on its own if it wants to cover the request or not.
   * </p>
   *
   * @param request a request to be handled.
   * @return Either <code>null</code> or {@link Response} object. If this service doesn't cover this
   *         particular request (for example the request is for a different domain), then
   *         <code>null</code> should be returned.
   * @throws IOException if an error occurred during handling (or the service wants to simulate that
   *         error).
   */
  Response handleInternetRequest(Request request) throws IOException;
}
