package eu.erasmuswithoutpaper.registry.iia;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.xml.security.Init;
import org.apache.xml.security.c14n.Canonicalizer;
import org.apache.xml.security.exceptions.XMLSecurityException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

@Service
public class IiaHashService {
  private final XPathExpression xpathIiasExpr;
  private final XPathExpression xpathCooperationConditionsExpr;
  private final XPathExpression xpathCooperationConditionsHashExpr;

  @Value("${app.registry-repo-base-url}")
  private String registryRepoBaseUrl;

  private final String iiasNs;

  IiaHashService() throws XPathExpressionException {
    this.iiasNs = registryRepoBaseUrl
        + "/ewp-specs-api-iias/blob/stable-v6/endpoints/get-response.xsd";

    XPathFactory xpathFactory = XPathFactory.newInstance();
    XPath xpath = xpathFactory.newXPath();
    xpath.setNamespaceContext(new IiaNamespaceContext(iiasNs));

    xpathIiasExpr = xpath.compile("/iia:iias-get-response/iia:iia");
    xpathCooperationConditionsExpr = xpath.compile("iia:cooperation-conditions");
    xpathCooperationConditionsHashExpr = xpath.compile("iia:conditions-hash/text()");
  }

  private static byte[] getDataToHash(Node element) throws ElementHashException {
    try {
      Init.init();
      Canonicalizer canon =
          Canonicalizer.getInstance(Canonicalizer.ALGO_ID_C14N_EXCL_OMIT_COMMENTS);
      ByteArrayOutputStream canonicalWriter = new ByteArrayOutputStream();
      canon.canonicalizeSubtree(element, canonicalWriter);

      return canonicalWriter.toByteArray();
    } catch (XMLSecurityException cause) {
      throw new ElementHashException(cause);
    }
  }

  private Document getDocument(InputSource xml)
      throws ParserConfigurationException, SAXException, IOException {
    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    dbf.setNamespaceAware(true);
    DocumentBuilder db = dbf.newDocumentBuilder();
    return db.parse(xml);
  }

  private void removeContacts(Node cooperationConditions) {
    NodeList mobilitySpecs = cooperationConditions.getChildNodes();
    for (int i = 0; i < mobilitySpecs.getLength(); i++) {
      Node node = mobilitySpecs.item(i);
      if (node.getNodeType() != Node.ELEMENT_NODE) {
        continue;
      }

      Element mobilitySpec = (Element) node;
      NodeList sendingContacts = mobilitySpec.getElementsByTagNameNS(iiasNs, "sending-contact");
      for (int j = sendingContacts.getLength() - 1; j >= 0; j--) {
        mobilitySpec.removeChild(sendingContacts.item(j));
      }

      NodeList receivingContacts =
          mobilitySpec.getElementsByTagNameNS(iiasNs, "receiving-contact");
      for (int j = receivingContacts.getLength() - 1; j >= 0; j--) {
        mobilitySpec.removeChild(receivingContacts.item(j));
      }
    }
  }

  /**
   * Checks cooperation conditions hash present in IIA get response.
   *
   * @param iiaXml XML containing IIA get response
   * @return list of cooperation conditions hash comparison results (one for every IIA)
   * @throws ElementHashException when hash cannot be calculated
   */
  public List<HashComparisonResult> checkCooperationConditionsHash(InputSource iiaXml)
      throws ElementHashException, ParserConfigurationException, IOException, SAXException,
      XPathExpressionException {
    Document document = getDocument(iiaXml);

    NodeList iias = (NodeList) xpathIiasExpr.evaluate(document, XPathConstants.NODESET);
    ArrayList<HashComparisonResult> hashComparisonResults = new ArrayList<>(iias.getLength());
    for (int i = 0; i < iias.getLength(); i++) {
      hashComparisonResults.add(getHashComparisonResult(iias.item(i)));
    }

    return hashComparisonResults;
  }

  private HashComparisonResult getHashComparisonResult(Node iia)
      throws XPathExpressionException, ElementHashException {
    String hashExtracted = xpathCooperationConditionsHashExpr.evaluate(iia);
    Node cooperationConditions =
        (Node) xpathCooperationConditionsExpr.evaluate(iia, XPathConstants.NODE);
    removeContacts(cooperationConditions);
    byte[] dataToHash = getDataToHash(cooperationConditions);
    String hashExpected = DigestUtils.sha256Hex(dataToHash);

    return new HashComparisonResult(hashExtracted, hashExpected,
        new String(dataToHash, StandardCharsets.UTF_8));
  }

  static class IiaNamespaceContext implements NamespaceContext {

    private final String iiasNs;

    public IiaNamespaceContext(String iiasNs) {
      this.iiasNs = iiasNs;
    }

    @Override
    public String getNamespaceURI(String prefix) {
      if ("iia".equals(prefix)) {
        return iiasNs;
      }
      throw new IllegalArgumentException(prefix);
    }

    @Override
    public String getPrefix(String namespaceUri) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Iterator<String> getPrefixes(String namespaceUri) {
      throw new UnsupportedOperationException();
    }
  }
}
