package eu.erasmuswithoutpaper.registry.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.joox.JOOX.$;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import eu.erasmuswithoutpaper.registry.WRTest;
import eu.erasmuswithoutpaper.registry.documentbuilder.KnownNamespace;

import org.joox.Match;
import org.junit.Test;
import org.xml.sax.SAXException;

/**
 * Tests for {@link Utils} methods.
 */
public class UtilsTest extends WRTest {

  public void testHeaderNameFormatter() {
    assertThat(Utils.formatHeaderName("abc-def--XYZZ")).isEqualTo("Abc-Def--Xyzz");
    assertThat(Utils.formatHeaderName("---")).isEqualTo("---");
    assertThat(Utils.formatHeaderName("Abc")).isEqualTo("Abc");
    assertThat(Utils.formatHeaderName("")).isEqualTo("");
  }

  /**
   * Test the {@link Utils#rewritePrefixes(org.w3c.dom.Element)} method.
   */
  @Test
  public void testRewritingPrefixes() {

    StringBuilder inp = new StringBuilder();
    StringBuilder exp = new StringBuilder();

    Runnable chk = () -> {
      try {
        Match doc = $(new ByteArrayInputStream(inp.toString().getBytes(StandardCharsets.UTF_8)));
        Utils.rewritePrefixes(doc.get(0));
        assertThat(doc.toString()).isEqualTo(exp.toString());
      } catch (SAXException | IOException e) {
        throw new RuntimeException(e);
      }
      inp.setLength(0);
      exp.setLength(0);
    };

    inp.append("<root xmlns=\"urn:a\"><x/></root>");
    exp.append("<root xmlns=\"urn:a\"><x/></root>");
    chk.run();

    String r = KnownNamespace.RESPONSE_REGISTRY_V1.getNamespaceUri();
    inp.append("<root xmlns=\"" + r + "\"><x/></root>");
    exp.append("<r:root xmlns:r=\"" + r + "\"><r:x/></r:root>");
    chk.run();

    inp.append("<r:root xmlns:r=\"urn:a\"><r:x/></r:root>");
    exp.append("<root xmlns=\"urn:a\"><x/></root>");
    chk.run();

    inp.append("<root xmlns=\"" + r + "\">");
    inp.append("<x:child xmlns:x=\"" + r + "\"/>");
    inp.append("</root>");
    exp.append("<r:root xmlns:r=\"" + r + "\">");
    exp.append("<r:child/>");
    exp.append("</r:root>");
    chk.run();

    inp.append("<x:root xmlns:x=\"" + r + "\">");
    inp.append("<x:child/>");
    inp.append("</x:root>");
    exp.append("<r:root xmlns:r=\"" + r + "\">");
    exp.append("<r:child/>");
    exp.append("</r:root>");
    chk.run();

    inp.append("<x:root xmlns:x=\"" + r + "\">");
    inp.append("<x:child xmlns:x=\"urn:x\"/>");
    inp.append("</x:root>");
    exp.append("<r:root xmlns:r=\"" + r + "\">");
    exp.append("<child xmlns=\"urn:x\"/>");
    exp.append("</r:root>");
    chk.run();

    inp.append("<x:root other=\"n\" xmlns:x=\"" + r + "\" xmlns:y=\"urn:y\" xmlns:z=\"urn:z\">");
    inp.append("<y:child z:other=\"z\"/>");
    inp.append("</x:root>");
    exp.append("<r:root other=\"n\" xmlns:r=\"" + r + "\">");
    exp.append("<child xmlns:z=\"urn:z\" z:other=\"z\" xmlns=\"urn:y\"/>");
    exp.append("</r:root>");
    chk.run();
  }
}
