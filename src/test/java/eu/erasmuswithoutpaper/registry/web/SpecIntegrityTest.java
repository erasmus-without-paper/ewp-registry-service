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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

  @BeforeEach
  public void setUp() {
    this.root =
        $(this.docBuilder.build(new BuildParams(this.getFile("catalogues/sample-catalogue.xml")))
            .getDocument().get().getDocumentElement()).namespaces(KnownNamespace.prefixMap());
  }

  /**
   * Test the XPath examples copied from the Registry API specs:
   *
   * https://github.com/erasmus-without-paper/ewp-specs-api-registry#examples-of-catalogue-data-
   * extraction
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
    elems = this.root.xpath("//r:client-credentials-in-use/r:certificate[@sha-256=\"" + f1 + "\"]")
        .get();
    assertThat(elems).hasSize(1);
    elems = this.root.xpath("//r:client-credentials-in-use/r:certificate[@sha-256=\"" + f1
        + "\"]/../../r:institutions-covered/r:hei-id").get();
    assertThat(elems).hasSize(2);
    assertThat(elems.get(0).getTextContent()).isEqualTo("example1.com");
    assertThat(elems.get(1).getTextContent()).isEqualTo("example2.com");
    elems = this.root.xpath("//r:client-credentials-in-use/r:certificate[@sha-256=\"" + f3
        + "\"]/../../r:institutions-covered/r:hei-id").get();
    assertThat(elems).hasSize(1);
    assertThat(elems.get(0).getTextContent()).isEqualTo("example2.com");

    // Question 3: I have received a request signed with HTTP Signature with
    // `keyId` equal to `X`. How do I retrieve the actual public key, which I can
    // later use to validate the request's signature?

    elems = this.root.xpath("//r:binaries/r:rsa-public-key[@sha-256=\"" + f1 + "\"]").get();
    assertThat(elems).hasSize(1);
    assertThat(elems.get(0).getTextContent()).isEqualTo("cc1");

    // Question 4: I have received a request signed with HTTP Signature with
    // `keyId` equal to `X`. I have already validated the signature (as described in
    // question 3), so I know that the sender is in possession of the private part
    // of the key-pair. How do I retrieve the list of HEIs who's data is this client
    // privileged to access?

    elems = this.root.xpath("//r:client-credentials-in-use/r:rsa-public-key[@sha-256=\"" + f1
        + "\"]/../../r:institutions-covered/r:hei-id").get();
    assertThat(elems).hasSize(2);
    assertThat(elems.get(0).getTextContent()).isEqualTo("example1.com");
    assertThat(elems.get(1).getTextContent()).isEqualTo("example2.com");
    elems = this.root.xpath("//r:client-credentials-in-use/r:rsa-public-key[@sha-256=\"" + f3
        + "\"]/../../r:institutions-covered/r:hei-id").get();
    assertThat(elems).hasSize(1);
    assertThat(elems.get(0).getTextContent()).isEqualTo("example2.com");

    // Question 5: I don't trust regular TLS Server Authentication and I want
    // to authenticate the server via HTTP signature. I have already found the
    // API entry `X`, extracted the endpoint's URL `Y` from it, and have
    // received the server's response which has been signed with `keyId=Z`. I
    // have already validated the signature (as described in question 3), so I
    // know that the sender is in possession of the private part of the
    // key-pair. How can I verify if `Z` is the correct key with which `Y`'s
    // responses should have been signed with?

    elems = this.root.xpath("//e1:echo[@version=\"1.1.0\"]").get();
    assertThat(elems).hasSize(1);
    Match x = $(elems.get(0)).namespaces(KnownNamespace.prefixMap());
    elems = x.xpath("./../../r:server-credentials-in-use/r:rsa-public-key[@sha-256=\"" + f1 + "\"]")
        .get();
    assertThat(elems).hasSize(1);
    elems = x.xpath("./../../r:server-credentials-in-use/r:rsa-public-key[@sha-256=\"" + f3 + "\"]")
        .get();
    assertThat(elems).hasSize(0);
  }
}
