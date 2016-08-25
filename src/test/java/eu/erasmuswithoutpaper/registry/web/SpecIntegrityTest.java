package eu.erasmuswithoutpaper.registry.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.joox.JOOX.$;

import java.util.List;

import eu.erasmuswithoutpaper.registry.WRTest;
import eu.erasmuswithoutpaper.registry.documentbuilder.BuildParams;
import eu.erasmuswithoutpaper.registry.documentbuilder.EwpDocBuilder;
import eu.erasmuswithoutpaper.registry.documentbuilder.KnownNamespace;

import org.springframework.beans.factory.annotation.Autowired;

import org.joox.Match;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Element;

/**
 * This set of tests is intended to help to keep the XPath examples in the Registry API specs in
 * sync with the reality. Whenever any of these tests needs to change, the examples in the Registry
 * API specs will need to change too.
 */
public class SpecIntegrityTest extends WRTest {

  @Autowired
  private EwpDocBuilder docBuilder;

  private Match root;

  @Before
  public void setUp() {
    this.root =
        $(this.docBuilder.build(new BuildParams(this.getFile("catalogues/sample-catalogue.xml")))
            .getDocument().get().getDocumentElement()).namespaces(KnownNamespace.prefixMap());
  }

  /**
   * Test the XPath examples copied from the Registry API specs:
   *
   * https://github.com/erasmus-without-paper/ewp-specs-api-registry/tree/v1.0.1#examples-of-
   * catalogue-data-extraction
   */
  @Test
  public void testRegistryXPathExamples() {

    // Question 1: At which URLs and in which versions API X is implemented for institution Y?

    List<Element> elems;
    elems = this.root.xpath("//r:hei-id[text()=\"example1.com\"]").get();
    assertThat(elems).hasSize(1);
    assertThat(elems).hasSize(1);
    elems = this.root.xpath("//r:hei-id[text()=\"example1.com\"]/../../r:apis-implemented/e1:echo")
        .get();
    assertThat(elems).hasSize(2);
    elems = this.root.xpath("//r:hei-id[text()=\"example2.com\"]/../../r:apis-implemented/e1:echo")
        .get();
    assertThat(elems).hasSize(3);

    // Question 2: I have received a HTTPS request signed by a client certificate cert. Data of
    // which HEIs is this client privileged to access?

    String f1 = "1111111111111111111111111111111111111111111111111111111111111111";
    String f3 = "3333333333333333333333333333333333333333333333333333333333333333";
    elems = this.root.xpath("//r:certificate[@sha-256=\"" + f1 + "\"]").get();
    assertThat(elems).hasSize(1);
    elems = this.root
        .xpath("//r:certificate[@sha-256=\"" + f1 + "\"]/../../r:institutions-covered/r:hei-id")
        .get();
    assertThat(elems).hasSize(2);
    assertThat(elems.get(0).getTextContent()).isEqualTo("example1.com");
    assertThat(elems.get(1).getTextContent()).isEqualTo("example2.com");
    elems = this.root
        .xpath("//r:certificate[@sha-256=\"" + f3 + "\"]/../../r:institutions-covered/r:hei-id")
        .get();
    assertThat(elems).hasSize(1);
    assertThat(elems.get(0).getTextContent()).isEqualTo("example2.com");
  }
}
