package eu.erasmuswithoutpaper.registry.updater;

import static org.joox.JOOX.$;
import static org.w3c.dom.Node.ELEMENT_NODE;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;

import eu.erasmuswithoutpaper.registry.common.Utils;
import eu.erasmuswithoutpaper.registry.documentbuilder.KnownElement;
import eu.erasmuswithoutpaper.registry.documentbuilder.KnownNamespace;

import com.google.common.base.Joiner;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * The class is responsible for building Registry catalogue documents (from a list of filtered
 * manifest documents).
 *
 * <p>
 * It's not a service, because it holds a non-thread-safe {@link #doc} field.
 * </p>
 */
class CatalogueBuilder {

  private static final Logger logger = LoggerFactory.getLogger(CatalogueBuilder.class);

  private final DocumentBuilder docbuilder;

  private Document doc;

  public CatalogueBuilder() {
    this.docbuilder = Utils.newSecureDocumentBuilder();
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
    Element catalogueElem = this.newElem("catalogue");
    this.doc.appendChild(catalogueElem);

    Map<String, Map<String, Set<String>>> heiIdTypeSets = new TreeMap<>();
    Map<String, Map<String, Set<String>>> heiLangNameSets = new TreeMap<>();
    Map<String, RSAPublicKey> actualKeys = new TreeMap<>();

    // For each of the given manifests...

    for (Document manifestDoc : manifests) {

      if (!KnownElement.RESPONSE_MANIFEST_V6.matches(manifestDoc.getDocumentElement())) {
        logger.error("Ignoring unsupported manifest version while building the catalogue. "
            + "This should not happen.");
        continue;
      }

      for (Element hostElement : $(manifestDoc).children()) {
        catalogueElem.appendChild(getHost(hostElement, heiIdTypeSets, heiLangNameSets, actualKeys));
      }
    }

    // Create and append the <institutions> element.

    Element institutions = this.newElem("institutions");
    catalogueElem.appendChild(institutions);

    // For each of the institutions found in the manifests...

    for (Map.Entry<String, Map<String, Set<String>>> entry : heiLangNameSets.entrySet()) {
      String heiId = entry.getKey();

      // Append a new <hei id='...'> element to <institutions>.

      Element hei = this.newElem("hei");
      institutions.appendChild(hei);
      hei.setAttribute("id", heiId);

      // For each type of <other-id> used for this HEI (among all the manifests)...

      Map<String, Set<String>> idTypeSets = heiIdTypeSets.get(heiId);
      for (Map.Entry<String, Set<String>> entry2 : idTypeSets.entrySet()) {

        // Create an <other-id> element for each unique ID used.

        for (String id : entry2.getValue()) {
          Element otherId = this.newElem("other-id", id);
          hei.appendChild(otherId);
          otherId.setAttribute("type", entry2.getKey());
        }
      }

      // For each language of <name> used for this HEI (among all the manifests)...

      Map<String, Set<String>> langNameSets = entry.getValue();
      for (Map.Entry<String, Set<String>> entry3 : langNameSets.entrySet()) {
        String lang = entry3.getKey();

        // Create a <name> element for each unique name used.

        for (String nameUsed : entry3.getValue()) {
          Element name = this.newElem("name", nameUsed);
          hei.appendChild(name);
          if (lang.length() > 0) {
            name.setAttributeNS(XMLConstants.XML_NS_URI, "xml:lang", lang);
          }
        }
      }
    }

    // Include all the previously referenced public keys.

    if (!actualKeys.isEmpty()) {
      Element binariesElem = this.newElem("binaries");
      catalogueElem.appendChild(binariesElem);
      for (Entry<String, RSAPublicKey> entry : actualKeys.entrySet()) {
        Element keyElem = this.newElem("rsa-public-key");
        binariesElem.appendChild(keyElem);
        keyElem.setAttribute("sha-256", entry.getKey());
        // We want binaries "pretty-printed" (chunked and indented).
        StringBuilder sb = new StringBuilder();
        sb.append('\n');
        for (String line : this.getBase64EncodedLines(entry.getValue().getEncoded())) {
          sb.append("            ");
          if (line.length() > 0) {
            sb.append(line);
            sb.append('\n');
          }
        }
        sb.append("        ");
        keyElem.setTextContent(sb.toString());
      }
    }

    // Add xmlns:xxx attributes for most of the KnownNamespace prefixes.

    List<String> chunks = new ArrayList<String>();
    for (KnownNamespace ns : KnownNamespace.values()) {
      if (ns.isToBeIncludedInCatalogueXmlns() && ns != KnownNamespace.RESPONSE_REGISTRY_V1) {
        catalogueElem.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI,
            "xmlns:" + ns.getPreferredPrefix(), ns.getNamespaceUri());
      }
      if (ns.isToBeIncludedInCatalogueXmlns()) {
        chunks.add(ns.getNamespaceUri() + "\n        " + ns.getDefaultSchemaLocation());
      }
    }

    // Compose a proper xsi:schemaLocation attribute.

    String schemaLocation = "\n        " + Joiner.on("\n\n        ").join(chunks) + "\n    ";
    catalogueElem.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns:xsi",
        XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI);
    catalogueElem.setAttributeNS("http://www.w3.org/2001/XMLSchema-instance", "xsi:schemaLocation",
        schemaLocation);

    return this.doc;
  }

  private Element getHost(Element srcHostElem, Map<String, Map<String, Set<String>>> heiIdTypeSets,
      Map<String, Map<String, Set<String>>> heiLangNameSets, Map<String, RSAPublicKey> actualKeys) {
    Element destHostElem = this.newElem("host");
    Element destApisElem = this.newElem("apis-implemented");
    Element destCliCreds = this.newElem("client-credentials-in-use");
    Element destSrvCreds = this.newElem("server-credentials-in-use");

    for (Node node : Utils.asNodeList(srcHostElem.getChildNodes())) {
      if ("admin-email".equals(node.getLocalName())) {
        destHostElem.appendChild(this.newEwpElem("admin-email", node.getTextContent()));
      }
      if ("admin-provider".equals(node.getLocalName())) {
        destHostElem.appendChild(this.newEwpElem("admin-provider", node.getTextContent()));
      }
      if ("admin-notes".equals(node.getLocalName())) {
        destHostElem.appendChild(this.newEwpElem("admin-notes", node.getTextContent()));
      }
      if ("apis-implemented".equals(node.getLocalName())) {
        destHostElem.appendChild(destApisElem);
        // Copy all API entries from the manifest (and replace their prefixes with the default ones)
        for (Node api : Utils.asNodeList(node.getChildNodes())) {
          if (api.getNodeType() == ELEMENT_NODE) {
            Element destApiElem = (Element) this.doc.importNode(api, true);
            Utils.rewritePrefixes(destApiElem);
            destApisElem.appendChild(destApiElem);
          }
        }
      }
      if ("institutions-covered".equals(node.getLocalName())) {
        destHostElem.appendChild(getInstitutionsCovered(node, heiIdTypeSets, heiLangNameSets));
      }
      if ("client-credentials-in-use".equals(node.getLocalName())) {
        addKey(node, actualKeys, destCliCreds);
      }
      if ("server-credentials-in-use".equals(node.getLocalName())) {
        addKey(node, actualKeys, destSrvCreds);
      }
    }

    if (destCliCreds.getChildNodes().getLength() > 0) {
      destHostElem.appendChild(destCliCreds);
    }
    if (destSrvCreds.getChildNodes().getLength() > 0) {
      destHostElem.appendChild(destSrvCreds);
    }

    return destHostElem;
  }

  private void addKey(Node node, Map<String, RSAPublicKey> actualKeys, Element destCreds) {
    for (Node credential : Utils.asNodeList(node.getChildNodes())) {
      if ("rsa-public-key".equals(credential.getLocalName())) {
        RSAPublicKey key = this.parseValidRsaPublicKey(credential.getTextContent());
        Element destKeyElem = this.newElem("rsa-public-key");
        String fingerprint = DigestUtils.sha256Hex(key.getEncoded());
        destKeyElem.setAttribute("sha-256", fingerprint);
        destCreds.appendChild(destKeyElem);
        actualKeys.put(fingerprint, key);
      }
    }
  }

  private Element getInstitutionsCovered(Node srcHeis,
      Map<String, Map<String, Set<String>>> heiIdTypeSets,
      Map<String, Map<String, Set<String>>> heiLangNameSets) {
    Element destHeisElem = this.newElem("institutions-covered");

    for (Node srcHei : Utils.asNodeList(srcHeis.getChildNodes())) {
      if ("hei".equals(srcHei.getLocalName())) {
        String id = srcHei.getAttributes().getNamedItem("id").getNodeValue();
        destHeisElem.appendChild(this.newElem("hei-id", id));

        if (!heiIdTypeSets.containsKey(id)) {
          heiIdTypeSets.put(id, new TreeMap<>());
        }
        Map<String, Set<String>> idTypeSets = heiIdTypeSets.get(id);
        if (!heiLangNameSets.containsKey(id)) {
          heiLangNameSets.put(id, new TreeMap<>());
        }
        Map<String, Set<String>> langNameSets = heiLangNameSets.get(id);

        for (Node heiChild : Utils.asNodeList(srcHei.getChildNodes())) {
          if ("other-id".equals(heiChild.getLocalName())) {
            String idType = heiChild.getAttributes().getNamedItem("type").getNodeValue();
            if (!idTypeSets.containsKey(idType)) {
              idTypeSets.put(idType, new TreeSet<>());
            }
            Set<String> set = idTypeSets.get(idType);
            set.add(heiChild.getTextContent());
          }

          if ("name".equals(heiChild.getLocalName())) {
            Node langAttribute =
                heiChild.getAttributes().getNamedItemNS(XMLConstants.XML_NS_URI, "lang");
            String lang = langAttribute != null ? langAttribute.getNodeValue() : "";
            if (!langNameSets.containsKey(lang)) {
              langNameSets.put(lang, new TreeSet<>());
            }
            Set<String> set = langNameSets.get(lang);
            set.add(heiChild.getTextContent());
          }
        }
      }
    }

    return destHeisElem;
  }

  private String[] getBase64EncodedLines(byte[] data) {
    Base64 encoder = new Base64(76, new byte[] { '\n' });
    String encoded = encoder.encodeToString(data);
    return encoded.split("\\n");
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

  private synchronized RSAPublicKey parseValidRsaPublicKey(String keyStr) {

    keyStr = keyStr.replaceAll("\\s+", "");
    byte[] decoded = Base64.decodeBase64(keyStr);

    try {
      return (RSAPublicKey) KeyFactory.getInstance("RSA")
          .generatePublic(new X509EncodedKeySpec(decoded));
    } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
      // This method assumes that input is already checked and valid.
      throw new RuntimeException(e);
    }
  }
}
