package eu.erasmuswithoutpaper.registry.validators;

import static org.joox.JOOX.$;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import eu.erasmuswithoutpaper.registry.internet.Request;
import eu.erasmuswithoutpaper.registry.internet.Response;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.joox.Match;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * Our internal base class for quickly creating validation steps implementing the
 * {@link ValidationStepWithStatus} interface.
 *
 * <p>
 * It is named "inline", because we generally intended for subclasses of this class to be created
 * inline (in {@link AbstractValidationSuite}). You only need to provide the {@link #innerRun()}
 * implementation, and then call {@link #run()} to run the test/step (and change the status and
 * message of this {@link ValidationStepWithStatus}).
 * </p>
 */
public abstract class InlineValidationStep implements ValidationStepWithStatus {

  @SuppressWarnings("serial")
  public static class Failure extends Exception {

    private final Status status;
    private transient Response serverResponse = null;

    /**
     * Create a Failure with certain message and status for given response.
     */
    public Failure(String message, Status status, Response serverResponse) {
      super(message);
      this.status = status;
      this.serverResponse = serverResponse;
    }

    public void attachServerResponse(Response response) {
      this.serverResponse = response;
    }

    public Optional<Response> getAttachedServerResponse() {
      return Optional.ofNullable(this.serverResponse);
    }

    public Status getStatus() {
      return this.status;
    }

    public Failure withChangedStatus(Status status) {
      return new Failure(this.getMessage(), status, this.serverResponse);
    }
  }

  private Status status = Status.PENDING;
  private String message = null;
  private final List<Request> requestSnapshots = new ArrayList<>();
  private final List<Response> responseSnapshots = new ArrayList<>();

  @Override
  public String getMessage() {
    if (this.message != null) {
      return this.message;
    } else {
      return "OK";
    }
  }

  @Override
  public List<Request> getRequestSnapshots() {
    return this.requestSnapshots;
  }

  @Override
  public List<Response> getResponseSnapshots() {
    return this.responseSnapshots;
  }

  @Override
  public Optional<String> getServerDeveloperErrorMessage() {
    List<Response> snapshots = this.getResponseSnapshots();
    if (snapshots.size() == 0) {
      return Optional.empty();
    }
    Document document;
    try {
      document =
          $(new ByteArrayInputStream(snapshots.get(snapshots.size() - 1).getBody())).document();
    } catch (SAXException | IOException e) {
      return Optional.empty();
    }
    Match root = $(document);
    if (root.find("developer-message").isNotEmpty()) {
      return Optional.of(root.find("developer-message").text());
    } else {
      return Optional.empty();
    }
  }

  @Override
  public Status getStatus() {
    return this.status;
  }

  void addRequestSnapshot(Request request) {
    if ((this.requestSnapshots.size() > 0)
        && (request.equals(this.requestSnapshots.get(this.requestSnapshots.size() - 1)))) {
      // Adding the same snapshot twice. Skip it.
      return;
    }
    this.requestSnapshots.add(new Request(request));
  }

  void addResponseSnapshot(Response response) {
    if ((this.responseSnapshots.size() > 0)
        && (response.equals(this.responseSnapshots.get(this.responseSnapshots.size() - 1)))) {
      // Adding the same snapshot twice. Skip it.
      return;
    }
    this.responseSnapshots.add(new Response(response));
  }

  /**
   * If it returns normally, and no other status is set during execution, then the status will be
   * set to SUCCESS.
   *
   * @return Optional server response object.
   * @throws Failure When a step fails, and its status is NOT supposed to be set to SUCCESS. The
   *         instance contains the both the error message and status to be used instead.
   */
  protected abstract Optional<Response> innerRun() throws Failure;

  protected void setMessage(String message) {
    this.message = message;
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
  public final Status run() {
    try {
      Optional<Response> response = this.innerRun();
      if (this.getStatus().equals(Status.PENDING)) {
        this.setStatus(Status.SUCCESS);
      }
      if (response.isPresent()) {
        this.addResponseSnapshot(response.get());
      }
    } catch (RuntimeException e) {
      this.setStatus(Status.ERROR);
      this.setMessage("Error: " + ExceptionUtils.getStackTrace(e));
    } catch (Failure e) {
      this.setStatus(e.getStatus());
      this.setMessage(e.getMessage());
      if (e.getAttachedServerResponse().isPresent()) {
        this.addResponseSnapshot(e.getAttachedServerResponse().get());
      }
    }
    return this.getStatus();
  }
}
