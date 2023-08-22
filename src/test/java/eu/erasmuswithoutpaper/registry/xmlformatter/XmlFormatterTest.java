package eu.erasmuswithoutpaper.registry.xmlformatter;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.xml.parsers.DocumentBuilder;

import eu.erasmuswithoutpaper.registry.WRTest;
import eu.erasmuswithoutpaper.registry.common.Utils;

import org.springframework.beans.factory.annotation.Autowired;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * Tests for {@link XmlFormatter}.
 */
public class XmlFormatterTest extends WRTest {

  @Autowired
  private XmlFormatter xmlFormatter;

  @Test
  public void test1() {
    this.chk("<xml/>", "<xml/>\n");
  }

  @Test
  public void test2() {
    this.chk("<xml attr='test'/>", "<xml attr=\"test\"/>\n");
  }

  @Test
  public void test3() {
    this.chk("<xml attr1='test' attr2='test'/>",
        "<xml\n    attr1=\"test\"\n    attr2=\"test\"\n/>\n");
  }

  @Test
  public void testA() {
    this.chk(this.getFileAsString("formatter-tests/A-input.xml"),
        this.getFileAsString("formatter-tests/A-output.xml"));
  }

  /**
   * Check if, for given input, the formatter gives an expected output.
   */
  private void chk(String input, String expectedOutput) {
    DocumentBuilder docbuilder = Utils.newSecureDocumentBuilder();
    Document doc;
    try {
      doc = docbuilder.parse(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));
    } catch (SAXException | IOException e) {
      throw new RuntimeException(e);
    }
    String formatted = this.xmlFormatter.format(doc);
    assertThat(formatted).isEqualTo(expectedOutput);
  }
}
