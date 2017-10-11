package eu.erasmuswithoutpaper.registry.internet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import eu.erasmuswithoutpaper.registry.WRTest;
import eu.erasmuswithoutpaper.registry.internet.FakeInternet.MultipleHandlersConflict;
import eu.erasmuswithoutpaper.registry.internet.Internet.Request;
import eu.erasmuswithoutpaper.registry.internet.Internet.Response;

import org.springframework.beans.factory.annotation.Autowired;

import org.assertj.core.util.Maps;
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

  @Test
  public void testFakeServices() {
    assertThat(this.fetchString(url1)).isNull();
    assertThat(this.fetchString(url2)).isNull();

    try {

      FakeInternetService service1 = new FakeInternetService() {
        @Override
        public Response handleInternetRequest(Request request) throws IOException {
          if (request.getUrl().equals(url1)) {
            return new Response(200, "It works!".getBytes(StandardCharsets.UTF_8));
          } else {
            return null;
          }
        }
      };

      this.internet.addFakeInternetService(service1);
      assertThat(this.fetchString(url1)).isEqualTo("It works!");
      assertThat(this.fetchString(url2)).isNull();

      FakeInternetService service2 = new FakeInternetService() {
        @Override
        public Response handleInternetRequest(Request request) throws IOException {
          // This service always responds, it doesn't verify if the request is for its domain.
          return new Response(200, "I'm a bad service!".getBytes(StandardCharsets.UTF_8),
              Maps.newHashMap("Special-Header", Arrays.asList("Special Value")));
        }
      };

      this.internet.addFakeInternetService(service2);
      try {
        this.fetchString(url1);
        fail("Exception expected");
      } catch (MultipleHandlersConflict e) {
        // Expected.
      }
      // url2 should still work though, because there's only one service that handles it.
      assertThat(this.fetchString(url2)).isEqualTo("I'm a bad service!");

      // Also make sure that responses are returned correctly.

      try {
        Response response = this.internet.makeRequest(new Request("POST", url2));
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getBody().length)
            .isEqualTo("I'm a bad service!".getBytes(StandardCharsets.UTF_8).length);
        assertThat(response.getAllHeaders().get("Special-Header"))
            .isEqualTo(Arrays.asList("Special Value"));
      } catch (IOException e) {
        // Shouldn't happen.
        throw new RuntimeException(e);
      }

      // Adding exactly the same service twice should result in an error.

      try {
        this.internet.addFakeInternetService(service2);
        fail("Exception expected");
      } catch (RuntimeException e) {
        assertThat(e.getMessage()).contains("already been added");
      }

      // Let's put a conflicting url via the putUrl method, and expected the conflict
      // to be detected.

      this.internet.putURL(url2, "Conflicting contents");
      try {
        this.fetchString(url2);
        fail("Exception expected");
      } catch (MultipleHandlersConflict e) {
        // Expected.
      }

      // Remove the service, and expect the conflicts to disappear.

      this.internet.removeFakeInternetService(service2);
      assertThat(this.fetchString(url1)).isEqualTo("It works!");
      assertThat(this.fetchString(url2)).isEqualTo("Conflicting contents");

    } finally {

      // Clean up.

      this.internet.clearURLs();
      this.internet.clearFakeInternetServices();
    }
  }

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
