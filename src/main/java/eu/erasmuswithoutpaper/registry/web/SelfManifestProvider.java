package eu.erasmuswithoutpaper.registry.web;

import static org.joox.JOOX.$;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateEncodingException;
import java.util.List;

import eu.erasmuswithoutpaper.registry.Application;
import eu.erasmuswithoutpaper.registry.documentbuilder.BuildParams;
import eu.erasmuswithoutpaper.registry.documentbuilder.EwpDocBuilder;
import eu.erasmuswithoutpaper.registry.documentbuilder.KnownNamespace;
import eu.erasmuswithoutpaper.registry.echotester.EchoTester;
import eu.erasmuswithoutpaper.registry.xmlformatter.XmlFormatter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.joox.Match;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * This service provides a Discovery Manifest document for the Registry Service itself.
 *
 * <p>
 * The Registry Service is responsible for distributing the information about various manifests
 * hosted in the EWP Network, but the Registry Service itself <b>also</b> hosts its own manifest
 * (which <b>also</b> is included in the catalogue hosted by the Registry). This class is
 * responsible for generating this manifest.
 * </p>
 */
@Service
public class SelfManifestProvider {

  private final ResourceLoader res;
  private final EwpDocBuilder docBuilder;
  private final XmlFormatter formatter;
  private final List<String> adminEmails;
  private final String echoTesterCertEncoded;
  private final List<String> echoTesterHeiIDs;

  private volatile String cached = null;

  /**
   * @param res Needed to fetch the manifest template from application resources.
   * @param docBuilder Needed to build a {@link Document} out of the template.
   * @param formatter Needed to format the end document as XML.
   * @param adminEmails A list of email addresses, separated by commas. These addresses will be
   *        included in the <code>ewp:admin-email</code> elements in the generated manifest file.
   * @param echoTester Needed, because we need to publish its client credentials in our manifest.
   */
  @Autowired
  public SelfManifestProvider(ResourceLoader res, EwpDocBuilder docBuilder, XmlFormatter formatter,
      @Value("${app.admin-emails}") List<String> adminEmails, EchoTester echoTester) {
    this.res = res;
    this.docBuilder = docBuilder;
    this.formatter = formatter;
    this.adminEmails = adminEmails;
    try {
      this.echoTesterCertEncoded =
          new String(Base64.encodeBase64(echoTester.getTlsClientCertificateInUse().getEncoded()),
              StandardCharsets.US_ASCII);
    } catch (CertificateEncodingException e) {
      throw new RuntimeException(e);
    }
    this.echoTesterHeiIDs = echoTester.getCoveredHeiIDs();
  }

  /**
   * Return the manifest formatted in XML.
   *
   * @return A String with XML contents.
   */
  public String getManifest() {
    if (this.cached == null) {
      this.cached = this.generateManifest();
    }
    return this.cached;
  }

  private String generateManifest() {

    // Fetch the template.

    InputStream inputStream;
    try {
      inputStream = this.res.getResource("classpath:self-manifest-base.xml").getInputStream();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    Document doc;
    try {
      doc = this.docBuilder.build(new BuildParams(IOUtils.toByteArray(inputStream))).getDocument()
          .get();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    // Prepend <ewp:admin-email> elements.

    Element rootElem = doc.getDocumentElement();
    for (String email : Lists.reverse(this.adminEmails)) {
      Element elem =
          doc.createElementNS(KnownNamespace.COMMON_TYPES_V1.getNamespaceUri(), "ewp:admin-email");
      elem.setTextContent(email);
      rootElem.insertBefore(elem, rootElem.getFirstChild());
    }

    // Fix URLs.

    Match root = $(rootElem).namespaces(KnownNamespace.prefixMap());
    root.xpath("r:apis-implemented/d4:discovery/d4:url")
        .text(Application.getRootUrl() + "/manifest.xml");
    root.xpath("r:apis-implemented/r1:registry/r1:catalogue-url")
        .text(Application.getRootUrl() + "/catalogue-v1.xml");

    // Add covered HEIs.

    Element heisCoveredElem = (Element) rootElem.getElementsByTagNameNS(
        KnownNamespace.RESPONSE_MANIFEST_V4.getNamespaceUri(), "institutions-covered").item(0);
    for (String heiId : this.echoTesterHeiIDs) {
      Element heiElem =
          doc.createElementNS(KnownNamespace.RESPONSE_REGISTRY_V1.getNamespaceUri(), "hei");
      heiElem.setAttribute("id", heiId);
      Element nameElem =
          doc.createElementNS(KnownNamespace.RESPONSE_REGISTRY_V1.getNamespaceUri(), "name");
      nameElem.setTextContent("Artificial HEI for testing Echo APIs");
      heiElem.appendChild(nameElem);
      heisCoveredElem.appendChild(heiElem);
    }

    // Add TLS client certificates in use.

    Element credentialsElem = (Element) rootElem.getElementsByTagNameNS(
        KnownNamespace.RESPONSE_MANIFEST_V4.getNamespaceUri(), "client-credentials-in-use").item(0);
    Element certElem =
        doc.createElementNS(KnownNamespace.RESPONSE_MANIFEST_V4.getNamespaceUri(), "certificate");
    certElem.setTextContent(this.echoTesterCertEncoded);
    credentialsElem.appendChild(certElem);

    // Reformat.

    return this.formatter.format(doc);
  }
}
