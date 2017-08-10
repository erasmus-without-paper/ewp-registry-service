package eu.erasmuswithoutpaper.registry.echotester;

import java.util.Optional;

import eu.erasmuswithoutpaper.registry.internet.Internet;

/**
 * A single Echo API test along with its result.
 */
public interface EchoTestResult {

  /**
   * Possible outcomes of the test.
   */
  static enum Status {
    SUCCESS, NOTICE, WARNING, FAILURE, ERROR, PENDING
  }

  /**
   * @return The message to be displayed as the result of the test.
   */
  String getMessage();

  /**
   * @return The name (label) of this test.
   */
  String getName();

  /**
   * @return Optional response, as returned by server.
   */
  Optional<Internet.Response> getServerResponse();

  /**
   * @return The status of this test.
   */
  Status getStatus();
}
