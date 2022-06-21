package eu.erasmuswithoutpaper.registry.updater;

import static org.joox.JOOX.$;

import java.io.ByteArrayInputStream;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
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
import org.joox.Match;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.DOMException;
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
  private final CertificateFactory x509factory;

  private Document doc;

  public CatalogueBuilder() {
    try {
      this.docbuilder = Utils.newSecureDocumentBuilder();
      this.x509factory = CertificateFactory.getInstance("X.509");
    } catch (CertificateException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Build a catalogue document from the given list of manifests.
   *
   * @param manifestsV5 List of {@link Document}s - each MUST contain a VALID (and already filtered)
   *        Discovery API Manifest document in version 5 (otherwise they will be ignored).
   * @return A new {@link Document} with a valid Registry catalogue response.
   */
  public synchronized Document build(List<Document> manifestsV5) {

    // Create a new document with the <catalogue> root.

    this.doc = this.docbuilder.newDocument();
    Element catalogueElem = this.newElem("catalogue");
    this.doc.appendChild(catalogueElem);

    Map<String, Map<String, Set<String>>> heiIdTypeSets = new TreeMap<>();
    Map<String, Map<String, Set<String>>> heiLangNameSets = new TreeMap<>();
    Map<String, RSAPublicKey> actualKeys = new TreeMap<>();

    // For each of the given manifests...

    for (Document manifestDoc : manifestsV5) {

      if (!KnownElement.RESPONSE_MANIFEST_V5.matches(manifestDoc.getDocumentElement())
          && !KnownElement.RESPONSE_MANIFEST_V6.matches(manifestDoc.getDocumentElement())) {
        logger.error("Ignoring unsupported manifest version while building the catalogue. "
            + "This should not happen.");
        continue;
      }

      // Extract all the hosts
      Match srcHosts =
          $(manifestDoc).namespaces(KnownNamespace.prefixMap()).xpath("mf5:host | mf6:host");

      for (Element srcHostElem : srcHosts) {
        Match srcHost = $(srcHostElem).namespaces(KnownNamespace.prefixMap());

        // Append a new <host> element to the <catalogue>.

        Element destHostElem = this.newElem("host");
        catalogueElem.appendChild(destHostElem);

        // Copy all <ewp:admin-email> and <ewp:admin-notes> values.

        for (String email : srcHost.xpath("ewp:admin-email").texts()) {
          destHostElem.appendChild(this.newEwpElem("admin-email", email));
        }
        if (srcHost.xpath("ewp:admin-notes").isNotEmpty()
            && (srcHost.xpath("ewp:admin-notes").text().length() > 0)) {
          destHostElem
              .appendChild(this.newEwpElem("admin-notes", srcHost.xpath("ewp:admin-notes").text()));
        }

        // Append a new <apis-implemented> element to the <host>.

        Element destApisElem = this.newElem("apis-implemented");
        destHostElem.appendChild(destApisElem);

        // Copy all API entries from the manifest (and replace their prefixes with the default
        // ones).

        for (Element srcApiElem : srcHost.xpath("r:apis-implemented/*")) {
          Element destApiElem = (Element) this.doc.importNode(srcApiElem, true);
          Utils.rewritePrefixes(destApiElem);
          destApisElem.appendChild(destApiElem);
        }

        // It there are any HEIs covered in the manifest...

        Match srcHeis =
            srcHost.xpath("mf5:institutions-covered/r:hei | mf6:institutions-covered/r:hei");
        if (srcHeis.size() > 0) {

          // Create a <institutions-covered> element in the <host>.

          Element destHeisElem = this.newElem("institutions-covered");
          destHostElem.appendChild(destHeisElem);

          for (Match srcHei : srcHeis.each()) {

            // Append <hei-id> elements to <institutions-covered>.

            String id = srcHei.attr("id");
            destHeisElem.appendChild(this.newElem("hei-id", id));

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

            for (Match otherId : srcHei.xpath("r:other-id").each()) {

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

            for (Match name : srcHei.xpath("r:name").each()) {

              // Find the set of all names declared for this language.

              String lang = name.get(0).getAttributeNS(XMLConstants.XML_NS_URI, "lang");
              if (!langNameSets.containsKey(lang)) {
                langNameSets.put(lang, new TreeSet<>());
              }
              Set<String> set = langNameSets.get(lang);

              // Add the name to this set.

              set.add(name.text());
            }
          }
        }

        // Create <client-credentials-in-use> in <host>.

        Element destCliCreds = this.newElem("client-credentials-in-use");
        destHostElem.appendChild(destCliCreds);

        // If there are any client certificates...

        List<String> srcCertStrs = srcHost.xpath("mf5:client-credentials-in-use/mf5:certificate | "
            + "mf6:client-credentials-in-use/mf6:certificate").texts();
        if (srcCertStrs.size() > 0) {

          // For each certificate, calculate its sha-256 fingerprint, create element, and append it.

          for (String srcCertStr : srcCertStrs) {
            X509Certificate cert = this.parseCert(srcCertStr);
            Element destCertElem = this.newElem("certificate");
            try {
              destCertElem.setAttribute("sha-256", DigestUtils.sha256Hex(cert.getEncoded()));
            } catch (CertificateEncodingException e) {
              throw new RuntimeException(e);
            } catch (DOMException e) {
              throw new RuntimeException(e);
            }
            destCliCreds.appendChild(destCertElem);
          }
        }

        // If there are any client public keys...

        List<String> srcKeyStrs =
            srcHost.xpath("mf5:client-credentials-in-use/mf5:rsa-public-key | "
                + "mf6:client-credentials-in-use/mf6:rsa-public-key").texts();
        if (srcKeyStrs.size() > 0) {

          // For each key, calculate its sha-256 fingerprint, create element, and append it.

          for (String srcKeyStr : srcKeyStrs) {
            RSAPublicKey key = this.parseValidRsaPublicKey(srcKeyStr);
            Element destKeyElem = this.newElem("rsa-public-key");
            String fingerprint = DigestUtils.sha256Hex(key.getEncoded());
            destKeyElem.setAttribute("sha-256", fingerprint);
            destCliCreds.appendChild(destKeyElem);
            actualKeys.put(fingerprint, key);
          }
        }

        // If credentials are still empty, then remove their empty container.

        if (destCliCreds.getChildNodes().getLength() == 0) {
          destHostElem.removeChild(destCliCreds);
        }

        // Create <server-credentials-in-use> in <host>.

        Element destSrvCreds = this.newElem("server-credentials-in-use");
        destHostElem.appendChild(destSrvCreds);

        // If there are any server public keys...

        srcKeyStrs = srcHost.xpath("mf5:server-credentials-in-use/mf5:rsa-public-key | "
            + "mf6:server-credentials-in-use/mf6:rsa-public-key").texts();
        if (srcKeyStrs.size() > 0) {

          // For each key, calculate its sha-256 fingerprint, create element, and append it.

          for (String srcKeyStr : srcKeyStrs) {
            RSAPublicKey key = this.parseValidRsaPublicKey(srcKeyStr);
            Element destKeyElem = this.newElem("rsa-public-key");
            String fingerprint = DigestUtils.sha256Hex(key.getEncoded());
            destKeyElem.setAttribute("sha-256", fingerprint);
            destSrvCreds.appendChild(destKeyElem);
            actualKeys.put(fingerprint, key);
          }
        }

        // If credentials are still empty, then remove their empty container.

        if (destSrvCreds.getChildNodes().getLength() == 0) {
          destHostElem.removeChild(destSrvCreds);
        }
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

  private synchronized X509Certificate parseCert(String certStr) {

    certStr = certStr.replaceAll("\\s+", "");
    byte[] decoded = Base64.decodeBase64(certStr);

    try {
      return (X509Certificate) this.x509factory
          .generateCertificate(new ByteArrayInputStream(decoded));
    } catch (CertificateException e) {
      // This method assumes that input is already checked and valid.
      throw new RuntimeException(e);
    }
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
