package eu.erasmuswithoutpaper.registry.updater;

import static org.joox.JOOX.$;

import java.util.List;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilder;

import eu.erasmuswithoutpaper.registry.common.Utils;
import eu.erasmuswithoutpaper.registry.documentbuilder.BuildParams;
import eu.erasmuswithoutpaper.registry.documentbuilder.BuildResult;
import eu.erasmuswithoutpaper.registry.documentbuilder.EwpDocBuilder;
import eu.erasmuswithoutpaper.registry.documentbuilder.KnownElement;
import eu.erasmuswithoutpaper.registry.documentbuilder.KnownNamespace;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.joox.Match;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * This service provides a way to upgrade older manifest versions to the latest version.
 */
@Service
public class ManifestConverter {

  /**
   * Thrown when a valid manifest document was expected (in any non-discontinued Discovery API
   * version), but the provided document was not a valid manifest.
   */
  @SuppressWarnings("serial")
  public abstract static class NotValidManifest extends Exception {

    /**
     * @return The list of error messages telling you what was wrong about the (presumed) manifest.
     */
    public abstract List<String> getErrorList();
  }

  @SuppressWarnings("serial")
  @SuppressFBWarnings("SE_BAD_FIELD")
  private static class NotValidEwpDocument extends NotValidManifest {

    private final BuildResult buildResult;

    private NotValidEwpDocument(BuildResult buildResult) {
      this.buildResult = buildResult;
    }

    /**
     * @return {@link BuildResult} returned by the internal {@link EwpDocBuilder}. Note, that it MAY
     *         be valid, but the parsed document might not be a manifest at all.
     */
    @Override
    public List<String> getErrorList() {
      return this.buildResult.getErrors().stream().map(Object::toString)
          .collect(Collectors.toList());
    }
  }

  @SuppressWarnings("serial")
  private static class ValidEwpDocumentButNotManifest extends NotValidManifest {

    private final String namespaceUri;
    private final String localName;

    private ValidEwpDocumentButNotManifest(Element root) {
      this.namespaceUri = root.getNamespaceURI();
      this.localName = root.getLocalName();
    }

    @Override
    public List<String> getErrorList() {
      return Lists.newArrayList("This is a valid {" + this.namespaceUri + "}" + this.localName
          + " document, but we are expecting a manifest.");
    }
  }

  private final EwpDocBuilder docBuilder;

  /**
   * @param docBuilder Needed to run XML schema validation on the provided manifest documents.
   */
  @Autowired
  public ManifestConverter(EwpDocBuilder docBuilder) {
    this.docBuilder = docBuilder;
  }

  /**
   * Take an XML manifest in ANY supported version and build a DOM {@link Document} with the most
   * recent version 5 of the manifest format.
   *
   * @param manifestXmlContents XML contents of the presumed manifest file.
   * @return A DOM {@link Document} with Manifest v5.
   * @throws NotValidManifest If the provided document was not a valid Manifest (in any of its
   *         supported versions).
   */
  public Document buildToV5(byte[] manifestXmlContents) throws NotValidManifest {

    // Try to parse it.

    BuildParams params = new BuildParams(manifestXmlContents);
    BuildResult result = this.docBuilder.build(params);

    if (!result.isValid()) {
      throw new NotValidEwpDocument(result);
    }

    // Verify the namespace.

    Document doc = result.getDocument().get();
    if (KnownElement.RESPONSE_MANIFEST_V4.matches(doc.getDocumentElement())) {
      // Older version. Convert it to version 5.
      return this.convertFromV4ToV5(doc);
    } else if (KnownElement.RESPONSE_MANIFEST_V5.matches(doc.getDocumentElement())) {
      // Already in version 5.
      return doc;
    } else {
      // Despite the document being valid in itself, it's not a manifest at all.
      // (For example, it can be a valid Registry response document.)
      throw new ValidEwpDocumentButNotManifest(doc.getDocumentElement());
    }
  }

  /**
   * Convert the manifest from version 4 to version 5.
   *
   * @param srcDoc DOM {@link Document} with a valid Manifest v4. It MUST be valid (according to v4
   *        XSD).
   * @return A new DOM {@link Document} with the converted Manifest v5.
   */
  public Document convertFromV4ToV5(Document srcDoc) {
    DocumentBuilder docBuilder = Utils.newSecureDocumentBuilder();
    Document destDoc = docBuilder.newDocument();
    Element destManifest =
        destDoc.createElementNS(KnownElement.RESPONSE_MANIFEST_V5.getNamespaceUri(),
            KnownElement.RESPONSE_MANIFEST_V5.getLocalName());
    destDoc.appendChild(destManifest);
    Element destHost = this.addElem(destManifest, "host");
    this.convertSingleHost($(srcDoc.getDocumentElement()).namespaces(KnownNamespace.prefixMap()),
        destHost);
    return destDoc;
  }

  private Element addElem(Element destParent, String qualifiedName) {
    return this.addElem(destParent, qualifiedName, KnownNamespace.RESPONSE_MANIFEST_V5);
  }

  private Element addElem(Element destParent, String qualifiedName, KnownNamespace namespace) {
    Element elem =
        destParent.getOwnerDocument().createElementNS(namespace.getNamespaceUri(), qualifiedName);
    destParent.appendChild(elem);
    return elem;
  }

  private void convertSingleHost(Match srcManifest, Element destHost) {
    Match srcHost = srcManifest;
    for (Element elem : srcHost.xpath("ewp:admin-email")) {
      this.importElem(destHost, elem);
    }
    for (Element elem : srcHost.xpath("ewp:admin-notes")) {
      this.importElem(destHost, elem);
    }
    if (srcHost.xpath("r:apis-implemented").size() != 0) {
      this.importElem(destHost, srcHost.xpath("r:apis-implemented").get(0));
    }
    if (srcHost.xpath("mf4:institutions-covered/r:hei").size() > 0) {
      Element destHeis = this.addElem(destHost, "institutions-covered");
      for (Element heiElem : srcHost.xpath("mf4:institutions-covered/r:hei")) {
        this.importElem(destHeis, heiElem);
      }
    }
    if (srcHost.xpath("mf4:client-credentials-in-use").isNotEmpty()) {
      Element destCredentials = this.addElem(destHost, "client-credentials-in-use");
      for (String cert : srcHost.xpath("mf4:client-credentials-in-use/mf4:certificate").texts()) {
        this.addElem(destCredentials, "certificate").setTextContent(cert);
      }
      for (String cert : srcHost.xpath("mf4:client-credentials-in-use/mf4:rsa-public-key")
          .texts()) {
        this.addElem(destCredentials, "rsa-public-key").setTextContent(cert);
      }
    }
    if (srcHost.xpath("mf4:server-credentials-in-use").isNotEmpty()) {
      Element destCredentials = this.addElem(destHost, "server-credentials-in-use");
      for (String cert : srcHost.xpath("mf4:server-credentials-in-use/mf4:rsa-public-key")
          .texts()) {
        this.addElem(destCredentials, "rsa-public-key").setTextContent(cert);
      }
    }
  }

  private Element importElem(Element destParent, Element srcElem) {
    Element imported = (Element) destParent.getOwnerDocument().importNode(srcElem, true);
    destParent.appendChild(imported);
    return imported;
  }
}
