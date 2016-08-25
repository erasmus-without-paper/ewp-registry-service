package eu.erasmuswithoutpaper.registry.web;

import static org.joox.JOOX.$;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import eu.erasmuswithoutpaper.registry.Application;
import eu.erasmuswithoutpaper.registry.documentbuilder.BuildParams;
import eu.erasmuswithoutpaper.registry.documentbuilder.EwpDocBuilder;
import eu.erasmuswithoutpaper.registry.documentbuilder.KnownNamespace;
import eu.erasmuswithoutpaper.registry.xmlformatter.XmlFormatter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;
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

  private volatile String cached = null;

  /**
   * @param res Needed to fetch the manifest template from application resources.
   * @param docBuilder Needed to build a {@link Document} out of the template.
   * @param formatter Needed to format the end document as XML.
   * @param adminEmails A list of email addresses, separated by commas. These addresses will be
   *        included in the <code>ewp:admin-email</code> elements in the generated manifest file.
   */
  @Autowired
  public SelfManifestProvider(ResourceLoader res, EwpDocBuilder docBuilder, XmlFormatter formatter,
      @Value("${app.admin-emails}") List<String> adminEmails) {
    this.res = res;
    this.docBuilder = docBuilder;
    this.formatter = formatter;
    this.adminEmails = adminEmails;
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

    // Reformat.

    return this.formatter.format(doc);
  }
}
