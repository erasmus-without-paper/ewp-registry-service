package eu.erasmuswithoutpaper.registry.echotester;

import org.apache.commons.lang.exception.ExceptionUtils;

/**
 * A helper class for creating {@link EchoTestResult}s.
 */
abstract class EchoTest implements EchoTestResult {

  @SuppressWarnings("serial")
  static class Failure extends Exception {

    private final Status status;

    Failure() {
      this("Failure");
    }

    Failure(String message) {
      super(message);
      this.status = Status.FAILURE;
    }

    Failure(String message, Status status) {
      super(message);
      this.status = status;
    }

    Status getStatus() {
      return this.status;
    }
  }

  private Status status = Status.PENDING;
  private String message = null;

  @Override
  public String getMessage() {
    if (this.message != null) {
      return this.message;
    } else {
      return "OK";
    }
  }

  @Override
  public Status getStatus() {
    return this.status;
  }

  /**
   * If it returns normally, and no other status is set during execution, then the status will be
   * set to SUCCESS.
   *
   * @throws Failure When a test fails, and its status is NOT supposed to be set to SUCCESS. The
   *         instance contains the both the error message and status to be used instead.
   */
  protected abstract void innerRun() throws Failure;

  protected void setMessage(String message) {
    this.message = message;
  }

  protected void setStatus(Status status) {
    this.status = status;
  }

  /**
   * It runs the actual {@link #innerRun()} method, but also catches both expected and unexpected
   * exceptions, producing proper test results.
   *
   * @return The new status of the test.
   */
  final Status run() {
    try {
      this.innerRun();
      if (this.getStatus().equals(Status.PENDING)) {
        this.setStatus(Status.SUCCESS);
      }
    } catch (RuntimeException e) {
      this.setStatus(Status.ERROR);
      this.setMessage("Error: " + ExceptionUtils.getStackTrace(e));
    } catch (Failure e) {
      this.setStatus(e.getStatus());
      this.setMessage(e.getMessage());
    }
    return this.getStatus();
  }
}
