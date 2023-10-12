package eu.erasmuswithoutpaper.registry.notifier;

import eu.erasmuswithoutpaper.registry.common.Severity;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * This entity is used by the {@link NotifierService} to store status information on the recipients
 * being notified.
 */
@Entity
@Table(name = "REG_NOTIFICATION_RECIPIENT_STATUSES")
class RecipientStatus {

  @Id
  private String email; // NOPMD

  @Column(name = "currently_reported_flag_state")
  private int currentlyReportedFlagStatus;

  /**
   * Needed by Hibernate.
   */
  public RecipientStatus() {
  }

  /**
   * Use this constructor to create a new {@link RecipientStatus}.
   *
   * @param email Email address of the recipient, used as a key.
   */
  // Probably Spotbugs bug, there was no error in Java 11, but there is in 17
  @SuppressFBWarnings("URF_UNREAD_FIELD")
  public RecipientStatus(String email) {
    this.email = email;
    this.setCurrentlyReportedFlagStatus(Severity.OK);
  }

  /**
   * @return The last {@link Severity} status about which this recipient has been notified. This can
   *         never be {@link Severity#UNDETERMINED}.
   */
  public Severity getCurrentlyReportedFlagStatus() {
    return Severity.fromIntegerValue(this.currentlyReportedFlagStatus);
  }

  /**
   * @param value see {@link #getCurrentlyReportedFlagStatus()}.
   */
  public void setCurrentlyReportedFlagStatus(Severity value) {
    if (value.equals(Severity.UNDETERMINED)) {
      throw new RuntimeException();
    }
    this.currentlyReportedFlagStatus = value.getIntegerValue();
  }
}
