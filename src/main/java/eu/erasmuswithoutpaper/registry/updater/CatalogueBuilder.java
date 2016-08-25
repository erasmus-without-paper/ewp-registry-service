package eu.erasmuswithoutpaper.registry.updater;

import static org.joox.JOOX.$;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import eu.erasmuswithoutpaper.registry.common.Utils;
import eu.erasmuswithoutpaper.registry.documentbuilder.KnownNamespace;

import com.google.common.base.Joiner;
import org.apache.commons.codec.digest.DigestUtils;
import org.joox.Match;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * The class is responsible for building Registry catalogue documents (from a list of filtered
 * manifest documents).
 */
class CatalogueBuilder {

  private final DocumentBuilder docbuilder;
  private final CertificateFactory x509factory;

  private Document doc;

  public CatalogueBuilder() {
    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setNamespaceAware(true);
      this.docbuilder = factory.newDocumentBuilder();
      this.x509factory = CertificateFactory.getInstance("X.509");
    } catch (ParserConfigurationException | CertificateException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Build a catalogue document from the given list of manifests.
   *
   * @param manifests List of {@link Document}s - each MUST contain a VALID (and already filtered)
   *        Discovery API Manifest document.
   * @return A new {@link Document} with a valid Registry catalogue response.
   */
  public synchronized Document build(List<Document> manifests) {

    // Create a new document with the <catalogue> root.

    this.doc = this.docbuilder.newDocument();
    Element catalogue = this.newElem("catalogue");
    this.doc.appendChild(catalogue);

    Map<String, Map<String, Set<String>>> heiIdTypeSets = new TreeMap<>();
    Map<String, Map<String, Set<String>>> heiLangNameSets = new TreeMap<>();

    // For each of the given manifests...

    for (Document manifestDoc : manifests) {
      Match manifest = $(manifestDoc).namespaces(KnownNamespace.prefixMap());

      // Append a new <host> element to the <catalogue>.

      Element host = this.newElem("host");
      catalogue.appendChild(host);

      // Copy all <ewp:admin-email> and <ewp:admin-notes> values.

      for (String email : manifest.xpath("ewp:admin-email").texts()) {
        host.appendChild(this.newEwpElem("admin-email", email));
      }
      if (manifest.xpath("ewp:admin-notes").isNotEmpty()
          && (manifest.xpath("ewp:admin-notes").text().length() > 0)) {
        host.appendChild(this.newEwpElem("admin-notes", manifest.xpath("ewp:admin-notes").text()));
      }

      // Append a new <apis-implemented> element to the <host>.

      Element apisImplemented = this.newElem("apis-implemented");
      host.appendChild(apisImplemented);

      // Copy all API entries from the manifest (and replace their prefixes with the default ones).

      for (Element api : manifest.xpath("r:apis-implemented/*")) {
        Element newElement = (Element) this.doc.importNode(api, true);
        Utils.rewritePrefixes(newElement);
        apisImplemented.appendChild(newElement);
      }

      // It there are any HEIs covered in the manifest...

      Match heiElems = manifest.xpath("mf:institutions-covered/r:hei");
      if (heiElems.size() > 0) {

        // Create a <institutions-covered> element in the <host>.

        Element heisCovered = this.newElem("institutions-covered");
        host.appendChild(heisCovered);

        for (Match hei : heiElems.each()) {

          // Append <hei-id> elements to <institutions-covered>.

          String id = hei.attr("id");
          heisCovered.appendChild(this.newElem("hei-id", id));

          // And keep a copy of all relevant HEI attributes in our maps...

          if (!heiIdTypeSets.containsKey(id)) {
            heiIdTypeSets.put(id, new TreeMap<>());
          }
          Map<String, Set<String>> idTypeSets = heiIdTypeSets.get(id);
          if (!heiLangNameSets.containsKey(id)) {
            heiLangNameSets.put(id, new TreeMap<>());
          }
          Map<String, Set<String>> langNameSets = heiLangNameSets.get(id);

          // For each <other-id> given for this HEI...

          for (Match otherId : hei.xpath("r:other-id").each()) {

            // Find the set of all IDs declared for this ID type.

            String idType = otherId.attr("type");
            if (!idTypeSets.containsKey(idType)) {
              idTypeSets.put(idType, new TreeSet<>());
            }
            Set<String> set = idTypeSets.get(idType);

            // Add the ID to this set.

            set.add(otherId.text());
          }

          // For each <name> given for this HEI...

          for (Match name : hei.xpath("r:name").each()) {

            // Find the set of all names declared for this language.

            String lang = name.get(0).getAttributeNS(XMLConstants.XML_NS_URI, "lang");
            if (lang == null) {
              lang = ""; // no xml:lang specified
            }
            if (!langNameSets.containsKey(lang)) {
              langNameSets.put(lang, new TreeSet<>());
            }
            Set<String> set = langNameSets.get(lang);

            // Add the name to this set.

            set.add(name.text());
          }
        }
      }

      // It there are any certificates...

      List<String> certStrs = manifest.xpath("mf:client-credentials-in-use/mf:certificate").texts();
      if (certStrs.size() > 0) {

        // Create <client-credentials-in-use> in <host>.

        Element credentials = this.newElem("client-credentials-in-use");
        host.appendChild(credentials);

        // For each certificate, calculate its sha-256 fingerprint, create element, and append it.

        for (String certStr : certStrs) {
          X509Certificate cert = this.parseCert(certStr);
          Element certNode = this.newElem("certificate");
          try {
            certNode.setAttribute("sha-256", DigestUtils.sha256Hex(cert.getEncoded()));
          } catch (CertificateEncodingException e) {
            throw new RuntimeException(e);
          } catch (DOMException e) {
            throw new RuntimeException(e);
          }
          credentials.appendChild(certNode);
        }
      }
    }

    // Create and append the <institutions> element.

    Element institutions = this.newElem("institutions");
    catalogue.appendChild(institutions);

    // For each of the institutions found in the manifests...

    for (String heiId : heiLangNameSets.keySet()) {

      // Append a new <hei id='...'> element to <institutions>.

      Element hei = this.newElem("hei");
      institutions.appendChild(hei);
      hei.setAttribute("id", heiId);

      // For each type of <other-id> used for this HEI (among all the manifests)...

      Map<String, Set<String>> idTypeSets = heiIdTypeSets.get(heiId);
      for (String idType : idTypeSets.keySet()) {

        // Create an <other-id> element for each unique ID used.

        for (String id : idTypeSets.get(idType)) {
          Element otherId = this.newElem("other-id", id);
          hei.appendChild(otherId);
          otherId.setAttribute("type", idType);
        }
      }

      // For each language of <name> used for this HEI (among all the manifests)...

      Map<String, Set<String>> langNameSets = heiLangNameSets.get(heiId);
      for (String lang : langNameSets.keySet()) {

        // Create a <name> element for each unique name used.

        for (String nameUsed : langNameSets.get(lang)) {
          Element name = this.newElem("name", nameUsed);
          hei.appendChild(name);
          if (lang.length() > 0) {
            name.setAttributeNS(XMLConstants.XML_NS_URI, "xml:lang", lang);
          }
        }
      }
    }

    // Add xmlns:xxx attributes for each of the KnownNamespace prefixes.

    List<String> chunks = new ArrayList<String>();
    for (KnownNamespace ns : KnownNamespace.values()) {
      if (ns != KnownNamespace.RESPONSE_MANIFEST_V4 && ns != KnownNamespace.RESPONSE_REGISTRY_V1) {
        catalogue.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI,
            "xmlns:" + ns.getPreferredPrefix(), ns.getNamespaceUri());
      }
      if (ns != KnownNamespace.RESPONSE_MANIFEST_V4) {
        chunks.add(ns.getNamespaceUri() + "\n        " + ns.getDefaultSchemaLocation());
      }
    }

    // Compose a proper xsi:schemaLocation attribute.

    String schemaLocation = "\n        " + Joiner.on("\n\n        ").join(chunks) + "\n    ";
    catalogue.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns:xsi",
        XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI);
    catalogue.setAttributeNS("http://www.w3.org/2001/XMLSchema-instance", "xsi:schemaLocation",
        schemaLocation);

    return this.doc;
  }

  private synchronized Element newElem(String localName) {
    return this.doc.createElementNS(KnownNamespace.RESPONSE_REGISTRY_V1.getNamespaceUri(),
        localName);
  }

  private synchronized Element newElem(String localName, String content) {
    Element elem =
        this.doc.createElementNS(KnownNamespace.RESPONSE_REGISTRY_V1.getNamespaceUri(), localName);
    elem.appendChild(this.doc.createTextNode(content));
    return elem;
  }

  private synchronized Node newEwpElem(String localName, String content) {
    Element elem = this.doc.createElementNS(KnownNamespace.COMMON_TYPES_V1.getNamespaceUri(),
        KnownNamespace.COMMON_TYPES_V1.getPreferredPrefix() + ':' + localName);
    elem.appendChild(this.doc.createTextNode(content));
    return elem;
  }

  private synchronized X509Certificate parseCert(String certStr) {

    certStr = certStr.replaceAll("\\s+", "");
    byte[] decoded = Base64.getDecoder().decode(certStr);

    try {
      return (X509Certificate) this.x509factory
          .generateCertificate(new ByteArrayInputStream(decoded));
    } catch (CertificateException e) {
      throw new RuntimeException(e);
    }
  }
}
