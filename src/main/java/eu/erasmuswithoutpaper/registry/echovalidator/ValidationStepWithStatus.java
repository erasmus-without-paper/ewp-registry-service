package eu.erasmuswithoutpaper.registry.echovalidator;

import java.util.Optional;

import eu.erasmuswithoutpaper.registry.internet.Internet;

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
   * @return Optional response, as returned by server.
   */
  Optional<Internet.Response> getServerResponse();

  /**
   * @return Status of this validation step.
   */
  Status getStatus();
}
