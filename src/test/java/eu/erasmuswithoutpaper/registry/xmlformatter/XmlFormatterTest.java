package eu.erasmuswithoutpaper.registry.xmlformatter;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import javax.xml.parsers.DocumentBuilder;

import eu.erasmuswithoutpaper.registry.TestFiles;
import eu.erasmuswithoutpaper.registry.common.Utils;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

/**
 * Tests for {@link XmlFormatter}.
 */
public class XmlFormatterTest {

  private XmlFormatter xmlFormatter = new XmlFormatter();

  @Test
  public void test1() throws Exception {
    this.checkEquality("<xml/>", "<xml/>\n");
  }

  @Test
  public void test2() throws Exception {
    this.checkEquality("<xml attr='test'/>", "<xml attr=\"test\"/>\n");
  }

  @Test
  public void test3() throws Exception {
    this.checkEquality("<xml attr1='test' attr2='test'/>",
        "<xml\n    attr1=\"test\"\n    attr2=\"test\"\n/>\n");
  }

  @Test
  public void testA() throws Exception {
    this.checkEquality(TestFiles.getFileAsUtf8String("formatter-tests/A-input.xml"),
        TestFiles.getFileAsUtf8String("formatter-tests/A-output.xml"));
  }

  private void checkEquality(String input, String expectedOutput) throws Exception {
    DocumentBuilder docbuilder = Utils.newSecureDocumentBuilder();
    Document doc = docbuilder
        .parse(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));
    String formatted = this.xmlFormatter.format(doc);
    assertThat(formatted).isEqualTo(expectedOutput);
  }
}
