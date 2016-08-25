package eu.erasmuswithoutpaper.registry.constraints;

import eu.erasmuswithoutpaper.registry.common.Severity;

/**
 * An object describing a notice, warning or error in the manifest. It can also describe an action
 * taken in order to fix the manifest.
 */
public class FailedConstraintNotice {

  private final Severity severity;
  private final String messageHtml;

  /**
   * Create a new notice for the user about a failed constraint.
   *
   * @param severity see {@link #getSeverity()}.
   * @param messageHtml see {@link #getMessageHtml()}.
   */
  public FailedConstraintNotice(Severity severity, String messageHtml) {
    this.severity = severity;
    this.messageHtml = messageHtml;
  }

  /**
   * A message for the manifest author, formatted in HTML.
   *
   * <p>
   * It should describe what is (or was) wrong with the manifest, and what action has been taken in
   * order to fix the issue.
   * </p>
   *
   * @return HTML-formatted string.
   */
  public String getMessageHtml() {
    return this.messageHtml;
  }

  /**
   * Severity of the failure. It may influence the status of the manifest itself (e.g. existence of
   * errors may trigger a more serious notifications to be sent to the manifest author).
   *
   * @return One of the {@link Severity} constants.
   */
  public Severity getSeverity() {
    return this.severity;
  }

  @Override
  public String toString() {
    return this.getSeverity().toString() + ": " + this.getMessageHtml();
  }
}
