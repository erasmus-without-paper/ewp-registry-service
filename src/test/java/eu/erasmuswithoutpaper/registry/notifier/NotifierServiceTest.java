package eu.erasmuswithoutpaper.registry.notifier;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.List;

import eu.erasmuswithoutpaper.registry.WRTest;
import eu.erasmuswithoutpaper.registry.common.Severity;
import eu.erasmuswithoutpaper.registry.internet.FakeInternet;

import org.springframework.beans.factory.annotation.Autowired;

import org.assertj.core.util.Lists;
import org.junit.Test;

/**
 * Tests for {@link NotifierService}.
 */
public class NotifierServiceTest extends WRTest {

  @Autowired
  private FakeInternet internet;

  @Autowired
  private NotifierService notifier;

  /**
   * Run a complex scenario with a couple of {@link NotifierService} flags. Verify if all the
   * notifications are sent in correct places.
   */
  @Test
  public void testScenario1() {

    // Reset.

    this.notifier.removeAllWatchedFlags();
    this.internet.clearEmailsSent();

    // When application context is being loaded, other components register their own flags. Let's
    // register some.

    NotifierFlag flag1 = new NotifierFlag(Lists.newArrayList("john@example.com")) {
      @Override
      public String getName() {
        return "My flag 1";
      }
    };
    NotifierFlag flag2 =
        new NotifierFlag(Lists.newArrayList("john@example.com", "bob@example.com")) {
          @Override
          public String getName() {
            return "My flag 2";
          }
        };
    this.notifier.addWatchedFlag(flag1);
    this.notifier.addWatchedFlag(flag2);

    // At this point, the status of both flags is undetermined, so nobody should receive
    // any notifications.

    this.notifier.sendNotifications();
    List<String> sent = this.internet.popEmailsSent();
    assertThat(sent).hasSize(0);

    // Now, let's set flag2 to warning. Both recipients should receive notification.

    flag2.setStatus(Severity.WARNING);
    this.notifier.sendNotifications();
    sent = this.internet.popEmailsSent();
    Collections.sort(sent);
    assertThat(sent).hasSize(2);
    String content = sent.get(0);
    assertThat(content).isEqualTo(this.getFileAsString("emails/email1.txt")); // Bob
    content = sent.get(1);
    assertThat(content).contains("To: john@example.com");

    // However, if we switch flag2 back to OK, then only Bob should receive the notification,
    // because the status of flag1 is still undetermined.

    flag2.setStatus(Severity.OK);
    this.notifier.sendNotifications();
    sent = this.internet.popEmailsSent();
    assertThat(sent).hasSize(1);
    content = sent.get(0);
    assertThat(content).isEqualTo(this.getFileAsString("emails/email2.txt"));

    // Make sure no more notifications are sent when run again.

    this.notifier.sendNotifications();
    sent = this.internet.popEmailsSent();
    assertThat(sent).hasSize(0);

    // Report the same problem again. Bob will get the notification, John won't (because he
    // hasn't been notified that the severity of his problems has previously decreased).

    flag2.setStatus(Severity.WARNING);
    this.notifier.sendNotifications();
    sent = this.internet.popEmailsSent();
    assertThat(sent).hasSize(1);
    content = sent.get(0);
    assertThat(content).isEqualTo(this.getFileAsString("emails/email1.txt"));

    // Flag1 hasn't been determined since now. Let's set it to error. This change is not related to
    // Bob, but John should receive a notification.

    flag1.setStatus(Severity.ERROR);
    this.notifier.sendNotifications();
    sent = this.internet.popEmailsSent();
    assertThat(sent).hasSize(1);
    content = sent.get(0);
    assertThat(content).isEqualTo(this.getFileAsString("emails/email3.txt"));

    // Increase severity of flag2. Only Bob will receive notification (because the overall
    // severity didn't change for John).

    flag2.setStatus(Severity.ERROR);
    this.notifier.sendNotifications();
    sent = this.internet.popEmailsSent();
    assertThat(sent).hasSize(1);
    content = sent.get(0);
    assertThat(content).contains("To: bob@example.com");
    assertThat(content).contains("severity status has just *increased*");

    // A couple more tests.

    flag1.setStatus(Severity.WARNING);
    flag2.setStatus(Severity.UNDETERMINED);
    this.notifier.sendNotifications();
    sent = this.internet.popEmailsSent();
    assertThat(sent).hasSize(0);

    flag2.setStatus(Severity.OK);
    this.notifier.sendNotifications();
    sent = this.internet.popEmailsSent();
    Collections.sort(sent);
    assertThat(sent).hasSize(2);
    content = sent.get(0);
    assertThat(content).contains("To: bob@example.com");
    assertThat(content).contains("All problems seem to be resolved now");
    content = sent.get(1);
    assertThat(content).isEqualTo(this.getFileAsString("emails/email4.txt"));
  }
}
