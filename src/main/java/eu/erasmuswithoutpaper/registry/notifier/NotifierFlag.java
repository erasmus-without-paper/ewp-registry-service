package eu.erasmuswithoutpaper.registry.notifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import eu.erasmuswithoutpaper.registry.common.Severity;

import com.google.common.collect.ImmutableList;

/**
 * Flags are an abstract concept which describe "watched issues". Each flag has a name (see
 * {@link #getName()}), a list of recipients (see {@link #getRecipientEmails()} and a
 * {@link Severity} status (see {@link #getStatus()}.
 */
public abstract class NotifierFlag {

  private final List<String> rcptEmails;
  private volatile Severity status = Severity.UNDETERMINED;

  /**
   * Create a flag with an empty recipient list and a {@link Severity#UNDETERMINED} status.
   */
  public NotifierFlag() {
    this.rcptEmails = Collections.synchronizedList(new ArrayList<>());
  }

  /**
   * Create a flag with some recipients and a {@link Severity#UNDETERMINED} status.
   *
   * @param recipientEmails same as in {@link #setRecipientEmails(List)}.
   */
  public NotifierFlag(List<String> recipientEmails) {
    this.rcptEmails = recipientEmails;
  }

  /**
   * @return Optional URL of the page with the status details for this flag.
   */
  public Optional<String> getDetailsUrl() {
    return Optional.empty();
  }

  /**
   * Retrieve the name of the flag. This should shortly describe the issue being watched, e.g.
   * "Status of manifest XYZ.".
   *
   * @return A string with the short description.
   */
  public abstract String getName();

  /**
   * Retrieve the list of the recipients for this flag (this is a copy of the list, to avoid
   * thread-safety issues). This is the list of people which are "assigned" to watch the issue
   * described by this flag, and would like to be notified if something is wrong with it.
   *
   * @return A list of email addresses.
   */
  public final synchronized List<String> getRecipientEmails() {
    return ImmutableList.<String>builder().addAll(this.rcptEmails).build();
  }

  /**
   * @return The current status of the flag.
   */
  public Severity getStatus() {
    return this.status;
  }

  /**
   * Replace the list of recipients with another list (the contents of this list will be copied, to
   * avoid thread-safety issues).
   *
   * @param newEmails A list of email addresses.
   */
  public final synchronized void setRecipientEmails(List<String> newEmails) {
    this.rcptEmails.clear();
    this.rcptEmails.addAll(newEmails);
  }

  /**
   * Change the status of the flag.
   *
   * @param status The new status.
   */
  public void setStatus(Severity status) {
    this.status = status;
  }

  @Override
  public String toString() {
    return this.getName() + ": " + this.getStatus().toString();
  }
}
