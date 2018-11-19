package eu.erasmuswithoutpaper.registry.validators;

import java.util.List;
import java.util.Optional;

import eu.erasmuswithoutpaper.registry.internet.Request;
import eu.erasmuswithoutpaper.registry.internet.Response;

/**
 * Describes a single validation step, along with its result.
 */
public interface ValidationStepWithStatus {

  /**
   * Possible outcomes of the validation step.
   */
  static enum Status {
    SUCCESS, NOTICE, WARNING, FAILURE, ERROR, PENDING
  }

  /**
   * @return The message to be displayed as the result of the validation step.
   */
  String getMessage();

  /**
   * @return The name (label) of this validation step.
   */
  String getName();

  /**
   * @return List of request building snapshots. The first one contains only the body, then it's
   *         encoded (possibly multiple times, with each encoding taking one snapshot), and then
   *         it's signed. The last of these snapshots contains the request which has been ultimately
   *         sent to the server.
   */
  List<Request> getRequestSnapshots();

  /**
   * @return List of response decoding snapshots. The first one contains the response exactly as
   *         received from server. Then, signatures are verified and stripped. Then the response is
   *         decoded (possibly multiple times).
   */
  List<Response> getResponseSnapshots();

  /**
   * @return Optional error message, as parsed from the last of the {@link #getResponseSnapshots()}.
   */
  Optional<String> getServerDeveloperErrorMessage();

  /**
   * @return Status of this validation step.
   */
  Status getStatus();
}
