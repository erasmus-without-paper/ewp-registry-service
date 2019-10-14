package eu.erasmuswithoutpaper.registry.notifier;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import eu.erasmuswithoutpaper.registry.Application;
import eu.erasmuswithoutpaper.registry.common.Severity;
import eu.erasmuswithoutpaper.registry.common.Severity.OneOfTheValuesIsUndetermined;
import eu.erasmuswithoutpaper.registry.common.Utils;
import eu.erasmuswithoutpaper.registry.internet.Internet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This component watches the statuses and recipients of {@link NotifierFlag} instances registered
 * by other components (via {@link #addWatchedFlag(NotifierFlag)}). Then, whenever
 * {@link #sendNotifications()} is called, it notifies the recipients if there are any issues which
 * need their attention.
 */
@Component
@ConditionalOnWebApplication
public class NotifierService {

  private static final Logger logger = LoggerFactory.getLogger(NotifierService.class);

  private final RecipientStatusRepository rcptRepo;
  private final Internet internet;
  private final String instanceName;
  private final List<NotifierFlag> watchedFlags = new ArrayList<>();

  /**
   * @param rcptRepo {@link RecipientStatusRepository} to use for storing statuses of the
   *        recipients.
   * @param internet {@link Internet} instance to use for sending emails.
   * @param instanceName Name of the application to use in emails being sent.
   */
  @Autowired
  public NotifierService(RecipientStatusRepository rcptRepo, Internet internet,
      @Value("${app.instance-name}") String instanceName) {
    this.rcptRepo = rcptRepo;
    this.internet = internet;
    this.instanceName = instanceName;
  }

  /**
   * Add a new {@link NotifierFlag} to be watched.
   *
   * @param flag A {@link NotifierFlag} instance to be watched.
   */
  public synchronized void addWatchedFlag(NotifierFlag flag) {
    this.watchedFlags.add(flag);
  }

  /**
   * Get all flags with a severity greater than {@link Severity#OK}.
   *
   * @return A list of flags.
   */
  public synchronized List<NotifierFlag> getAllErroredFlags() {
    List<NotifierFlag> result = new ArrayList<>();
    for (NotifierFlag flag : this.watchedFlags) {
      try {
        if (flag.getStatus().isMoreSevereThan(Severity.OK)) {
          result.add(flag);
        }
      } catch (OneOfTheValuesIsUndetermined e) {
        continue;
      }
    }
    return result;
  }

  /**
   * Retrieve a list of all recipients associated with the watched flags.
   *
   * @return A unique list of email addresses.
   */
  public synchronized List<String> getAllRecipients() {
    Set<String> unique = new HashSet<>();
    for (NotifierFlag flag : this.watchedFlags) {
      unique.addAll(flag.getRecipientEmails());
    }
    return Lists.newArrayList(unique);
  }

  /**
   * Check the recipients of all flags (previously added by {@link #addWatchedFlag(NotifierFlag)})
   * and retrieve a list of all {@link NotifierFlag} instances which include a given email address
   * in their {@link NotifierFlag#getRecipientEmails()}.
   *
   * @param email An email address to look for.
   * @return A list of matching {@link NotifierFlag} instances.
   */
  public synchronized List<NotifierFlag> getFlagsWatchedBy(String email) {
    List<NotifierFlag> result = new ArrayList<>();
    for (NotifierFlag flag : this.watchedFlags) {
      if (flag.getRecipientEmails().contains(email)) {
        result.add(flag);
      }
    }
    return result;
  }

  /**
   * Clear the list of watched {@link NotifierFlag}s.
   */
  public synchronized void removeAllWatchedFlags() {
    this.watchedFlags.clear();
  }

  /**
   * Remove a {@link NotifierFlag} from the list of watched flags.
   *
   * @param flag A {@link NotifierFlag} instance to be removed.
   */
  public synchronized void removeWatchedFlag(NotifierFlag flag) {
    this.watchedFlags.remove(flag);
  }

  /**
   * Calling this method will cause the notifier to forget about all previously sent messages. This
   * will cause all warning and error messages to be resent, so this should be used only in tests.
   */
  public void resetAllRecipientStatuses() {
    this.rcptRepo.deleteAll();
  }

  /**
   * Check the statuses of all watched flags. Determine if any of the recipients need to be notified
   * about issues which need their attention. Send all notifications.
   *
   * <p>
   * Note that this method will attempt not to "spam" the users.
   * </p>
   */
  public synchronized void sendNotifications() {
    for (String email : this.getAllRecipients()) {

      // Determine what's the worst problem level visible to this recipient.

      Severity worstDeterminedStatus = Severity.OK;
      boolean someWereUndetermined = false;
      for (NotifierFlag flag : this.getFlagsWatchedBy(email)) {
        try {
          if (flag.getStatus().isMoreSevereThan(worstDeterminedStatus)) {
            worstDeterminedStatus = flag.getStatus();
          }
        } catch (OneOfTheValuesIsUndetermined e) {

          // One of the watched flags has an undetermined value. This may happen when
          // we attempt to send notifications promptly after application restart (before flags
          // were properly set up).

          someWereUndetermined = true;
        }
      }

      // What's the problem level this recipient has been last notified of?

      RecipientStatus rcpt = this.getOrCreateRecipient(email);
      Severity prevStatus = rcpt.getCurrentlyReportedFlagStatus();

      // Does the recipient require notification?

      boolean notified =
          this.judgeAndNotify(email, prevStatus, worstDeterminedStatus, someWereUndetermined);
      if (notified) {
        // The recipient has been notified about the change in the severity of his problems.
        rcpt.setCurrentlyReportedFlagStatus(worstDeterminedStatus);
        this.rcptRepo.save(rcpt);
      }
    }
  }

  private synchronized RecipientStatus getOrCreateRecipient(String email) {
    RecipientStatus result = this.rcptRepo.findOne(email);
    if (result == null) {
      result = new RecipientStatus(email);
    }
    return result;
  }

  /**
   * Given the "last reported" and the current status of the flags, judge it the user needs to be
   * notified, and notify him.
   *
   * @param email Email address of the user.
   * @param prevStatus A {@link Severity} about which the user has been notified in the last email
   *        sent to him. This cannot be {@link Severity#UNDETERMINED}!
   * @param worstDeterminedStatus The current highest severity of the non-
   *        {@link Severity#UNDETERMINED} flags watched by this user.
   * @param someWereUndetermined <b>true</b> if some of the flags watched by the user were
   *        {@link Severity#UNDETERMINED}.
   * @return <b>true</b> if we judged that the user needs to be notified, and a notification has
   *         been sent properly; <b>false</b> if we judged that the user doesn't need to be
   *         notified.
   */
  private boolean judgeAndNotify(String email, Severity prevStatus, Severity worstDeterminedStatus,
      boolean someWereUndetermined) {
    try {
      if (prevStatus.equals(worstDeterminedStatus)) {
        // Nothing changed since the last email was sent.
        return false;
      }
      StringBuilder sb = new StringBuilder();
      if (prevStatus.equals(Severity.OK)) {
        // From OK to WARNING or ERROR.
        sb.append("Hello " + email + ",\n\n");
        sb.append(this.instanceName);
        sb.append(" has reported problems in regard to one (or more)\n");
        sb.append("of the topics you've been assigned to.\n\n");
      } else {
        if (worstDeterminedStatus.isMoreSevereThan(prevStatus)) {
          // From WARNING to ERROR.
          sb.append("Reported severity status has just *increased*.\n\n");
        } else if (worstDeterminedStatus.equals(Severity.WARNING)) {
          // From ERROR to WARNING.
          if (someWereUndetermined) {
            // Some of flags are still in the undetermined state. We don't really know if
            // the severity has decreased. We will send no notifications.
            return false;
          } else {
            sb.append("Severity status has changed, but problems still exist.\n\n");
          }
        } else if (worstDeterminedStatus.equals(Severity.OK)) {
          // From WARNING or ERROR to OK.
          if (someWereUndetermined) {
            // Some of flags are still in the undetermined state. We don't really know if
            // the severity has decreased. We will send no notifications.
            return false;
          } else {
            sb.append("All problems seem to be resolved now! ");
            sb.append("We will notify you if they return.\n\n");
          }
        }
      }
      if (worstDeterminedStatus.isMoreSevereThan(Severity.OK)) {
        sb.append("Current severity status is \"" + worstDeterminedStatus.toString() + "\". ");
        sb.append("You will not receive further\n");
        sb.append("notifications unless this status changes.\n\n");
        sb.append("Visit your status page for details:\n");

        sb.append(Application.getRootUrl());
        sb.append("/status?email=");
        sb.append(Utils.urlencode(email));
        sb.append("\n\n");
      }
      sb.append("\n-- \n");
      sb.append(this.instanceName);
      sb.append('\n');
      sb.append(Application.getRootUrl());
      sb.append("/\n");
      String contents = sb.toString();

      logger.info("Sending \"" + worstDeterminedStatus.toString() + "\" notification to " + email);

      String subject = "EWP Status Update";
      ArrayList<String> recipients = Lists.newArrayList(email);
      this.internet.queueEmail(recipients, subject, contents);
      return true;
    } catch (OneOfTheValuesIsUndetermined e) {
      throw new RuntimeException(e);
    }
  }
}
