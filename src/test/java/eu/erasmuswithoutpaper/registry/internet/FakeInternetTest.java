package eu.erasmuswithoutpaper.registry.internet;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import eu.erasmuswithoutpaper.registry.WRTest;

import org.springframework.beans.factory.annotation.Autowired;

import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests for {@link FakeInternet}.
 */
public class FakeInternetTest extends WRTest {

  private static String url1;
  private static String url2;

  @BeforeClass
  public static void setUpClass() {
    url1 = "https://example.com/url1.xml";
    url2 = "https://example.com/url2.xml";
  }

  @Autowired
  private FakeInternet internet;

  /**
   * Some basic tests to verify that our implementation of {@link FakeInternet} is working as
   * expected.
   */
  @Test
  public void testUrlFetching() {

    // Initially, our internet does not have any URLs.

    assertThat(this.fetchString(url1)).isNull();
    assertThat(this.fetchString(url2)).isNull();

    // Put a content for one of the URLs.

    this.internet.putURL(url2, "url2");
    assertThat(this.fetchString(url1)).isNull();
    assertThat(this.fetchString(url2)).isEqualTo("url2");

    // Remove an URL which has never been put.

    this.internet.removeURL(url1);
    assertThat(this.fetchString(url2)).isEqualTo("url2");

    // Remove url2.

    this.internet.removeURL(url2);
    assertThat(this.fetchString(url2)).isNull();
  }

  private String fetchString(String url) {
    try {
      return new String(this.internet.getUrl(url), StandardCharsets.UTF_8);
    } catch (IOException e) {
      return null;
    }
  }
}
