package eu.erasmuswithoutpaper.registry.updater;

import static org.assertj.core.api.Assertions.assertThat;

import eu.erasmuswithoutpaper.registry.WRTest;
import eu.erasmuswithoutpaper.registry.internet.FakeInternet;

import org.springframework.beans.factory.annotation.Autowired;

import org.junit.Test;

/**
 * Tests for the {@link RegistryUpdaterImpl}.
 */
public class UptimeCheckerTest extends WRTest {

  @Autowired
  private FakeInternet internet;

  @Autowired
  private UptimeChecker checker;

  /**
   * Test if {@link UptimeChecker} understands the given XML format properly.
   */
  @Test
  public void verifyOutput() {
    String apiUrl = "https://api.uptimerobot.com/v2/getMonitors";
    this.internet.putURL(apiUrl, this.getFile("uptimerobot/response.xml"));
    assertThat(this.checker.getLast24HoursUptimeRatio()).isEqualTo("n/a");
    this.checker.refresh();
    assertThat(this.checker.getLast24HoursUptimeRatio()).isEqualTo("100.00%");
    assertThat(this.checker.getLast7DaysUptimeRatio()).isEqualTo("100.00%");
    assertThat(this.checker.getLast30DaysUptimeRatio()).isEqualTo("99.99%");
    assertThat(this.checker.getLast365DaysUptimeRatio()).isEqualTo("99.91%");
  }
}
