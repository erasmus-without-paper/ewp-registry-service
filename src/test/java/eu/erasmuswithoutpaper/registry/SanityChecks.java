package eu.erasmuswithoutpaper.registry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.joox.JOOX.$;

import java.io.IOException;
import java.io.StringReader;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import org.joox.JOOX;
import org.joox.Match;
import org.junit.Test;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * A couple of sanity checks, unrelated to any of the Registry components.
 */
public class SanityChecks extends WRTest {

  @Autowired
  private Environment environment;

  /**
   * Verify that our {@link JOOX} library is working as expected.
   */
  @Test
  public void testJOOX() {
    Match root;
    try {
      root = $(new InputSource(new StringReader("<a><b><c/><b/><c/></b></a>")));
    } catch (SAXException | IOException e) {
      throw new RuntimeException(e);
    }
    assertThat(root.size()).isEqualTo(1);
    assertThat(root.find("b").size()).isEqualTo(2);
    assertThat(root.xpath("b").size()).isEqualTo(1);
    assertThat(root.xpath("c").size()).isEqualTo(0);
    assertThat(root.xpath("b/c").size()).isEqualTo(2);
    assertThat(root.xpath("b/b").size()).isEqualTo(1);

    try {
      root = $(new InputSource(
          new StringReader("<a xmlns='urn:x'><b><c/><b xmlns='urn:y'/><c/></b></a>")));
    } catch (SAXException | IOException e) {
      throw new RuntimeException(e);
    }
    root = root.namespace("x", "urn:x");
    root = root.namespace("y", "urn:y");

    assertThat(root.size()).isEqualTo(1);
    assertThat(root.find("b").size()).isEqualTo(2);
    assertThat(root.xpath("x:b").size()).isEqualTo(1);
    assertThat(root.xpath("x:c").size()).isEqualTo(0);
    assertThat(root.xpath("x:b/x:c").size()).isEqualTo(2);
    assertThat(root.xpath("x:b/x:b").size()).isEqualTo(0);
    assertThat(root.xpath("x:b/y:b").size()).isEqualTo(1);

    assertThat(root.xpath("x:b/x:c | x:b/x:b").size()).isEqualTo(2);
    assertThat(root.xpath("x:b/x:c | x:b/y:b").size()).isEqualTo(3);
  }

  /**
   * Test if properties are properly overriden during unit tests.
   */
  @Test
  public void testPropertyOverriding() {
    assertThat(this.environment.getProperty("app.repo.enable-pushing")).isEqualTo("false");
    assertThat(this.environment.getProperty("app.admin-emails"))
        .startsWith("first-registry-admin@example.com");
  }
}
