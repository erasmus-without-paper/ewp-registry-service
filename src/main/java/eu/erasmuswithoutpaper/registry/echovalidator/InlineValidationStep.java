package eu.erasmuswithoutpaper.registry.echovalidator;

import java.util.Optional;

import eu.erasmuswithoutpaper.registry.internet.Internet;
import eu.erasmuswithoutpaper.registry.internet.Internet.Response;

import org.apache.commons.lang.exception.ExceptionUtils;

/**
 * Our internal base class for quickly creating validation steps implementing the
 * {@link ValidationStepWithStatus} interface.
 *
 * <p>
 * It is named "inline", because we generally intended for subclasses of this class to be created
 * inline (in {@link EchoValidationSuite}). You only need to provide the {@link #innerRun()}
 * implementation, and then call {@link #run()} to run the test/step (and change the status and
 * message of this {@link ValidationStepWithStatus}).
 * </p>
 */
abstract class InlineValidationStep implements ValidationStepWithStatus {

  @SuppressWarnings("serial")
  static class Failure extends Exception {

    private final Status status;
    private transient Internet.Response serverResponse = null;

    Failure(String message, Status status, Internet.Response serverResponse) {
      super(message);
      this.status = status;
      this.serverResponse = serverResponse;
    }

    void attachServerResponse(Internet.Response response) {
      this.serverResponse = response;
    }

    Optional<Internet.Response> getAttachedServerResponse() {
      return Optional.ofNullable(this.serverResponse);
    }

    Status getStatus() {
      return this.status;
    }
  }

  private Status status = Status.PENDING;
  private String message = null;
  private Internet.Response serverResponse = null;

  @Override
  public String getMessage() {
    if (this.message != null) {
      return this.message;
    } else {
      return "OK";
    }
  }

  @Override
  public Optional<Response> getServerResponse() {
    return Optional.ofNullable(this.serverResponse);
  }

  @Override
  public Status getStatus() {
    return this.status;
  }

  /**
   * If it returns normally, and no other status is set during execution, then the status will be
   * set to SUCCESS.
   *
   * @return Optional server response object.
   * @throws Failure When a step fails, and its status is NOT supposed to be set to SUCCESS. The
   *         instance contains the both the error message and status to be used instead.
   */
  protected abstract Optional<Internet.Response> innerRun() throws Failure;

  protected void setMessage(String message) {
    this.message = message;
  }

  protected void setServerResponse(Internet.Response response) {
    this.serverResponse = response;
  }

  protected void setStatus(Status status) {
    this.status = status;
  }

  /**
   * It runs the actual {@link #innerRun()} method, but also catches both expected and unexpected
   * exceptions, producing proper validation results.
   *
   * @return The new status of this validation step.
   */
  final Status run() {
    try {
      Optional<Response> response = this.innerRun();
      if (this.getStatus().equals(Status.PENDING)) {
        this.setStatus(Status.SUCCESS);
      }
      if (response.isPresent()) {
        this.setServerResponse(response.get());
      }
    } catch (RuntimeException e) {
      this.setStatus(Status.ERROR);
      this.setMessage("Error: " + ExceptionUtils.getStackTrace(e));
    } catch (Failure e) {
      this.setStatus(e.getStatus());
      this.setMessage(e.getMessage());
      if (e.getAttachedServerResponse().isPresent()) {
        this.setServerResponse(e.getAttachedServerResponse().get());
      }
    }
    return this.getStatus();
  }
}
