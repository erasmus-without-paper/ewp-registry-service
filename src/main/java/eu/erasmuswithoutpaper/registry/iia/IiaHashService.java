package eu.erasmuswithoutpaper.registry.iia;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import eu.erasmuswithoutpaper.registry.configuration.Constans;
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
  private final XPathExpression xpathIiasV6Expr;
  private final XPathExpression xpathIiasV7Expr;
  private final XPathExpression xpathCooperationConditionsExpr;
  private final XPathExpression xpathCooperationConditionsHashExpr;
  private final XPathExpression xpathIiaIdExpr;
  private final XPathExpression xpathIiaHashExpr;
  private final XPathExpression xpathResultIiaIdExpr;
  private final XPathExpression xpathResultIiaTextToHashExpr;

  private final String iiasV6Ns;

  IiaHashService() throws XPathExpressionException {
    this.iiasV6Ns = Constans.REGISTRY_REPO_URL
        + "/ewp-specs-api-iias/blob/stable-v6/endpoints/get-response.xsd";
    String iiasV7Ns = Constans.REGISTRY_REPO_URL
        + "/ewp-specs-api-iias/blob/stable-v7/endpoints/get-response.xsd";

    XPathFactory xpathFactory = XPathFactory.newInstance();
    XPath xpath = xpathFactory.newXPath();
    xpath.setNamespaceContext(new IiaNamespaceContext(iiasV6Ns, iiasV7Ns));

    xpathIiasV6Expr = xpath.compile("/iia6:iias-get-response/iia6:iia");
    xpathIiasV7Expr = xpath.compile("/iia7:iias-get-response/iia7:iia");
    xpathCooperationConditionsExpr = xpath.compile("iia6:cooperation-conditions");
    xpathCooperationConditionsHashExpr = xpath.compile("iia6:conditions-hash/text()");
    xpathIiaIdExpr = xpath.compile("iia7:partner/iia7:iia-id/text()");
    xpathIiaHashExpr = xpath.compile("iia7:iia-hash/text()");
    xpathResultIiaIdExpr = xpath.compile("iia-id/text()");
    xpathResultIiaTextToHashExpr = xpath.compile("text-to-hash/text()");
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
      NodeList sendingContacts = mobilitySpec.getElementsByTagNameNS(iiasV6Ns, "sending-contact");
      for (int j = sendingContacts.getLength() - 1; j >= 0; j--) {
        mobilitySpec.removeChild(sendingContacts.item(j));
      }

      NodeList receivingContacts =
          mobilitySpec.getElementsByTagNameNS(iiasV6Ns, "receiving-contact");
      for (int j = receivingContacts.getLength() - 1; j >= 0; j--) {
        mobilitySpec.removeChild(receivingContacts.item(j));
      }
    }
  }

  /**
   * Checks hash present in IIA get response. Both old and new hashes are handled.
   *
   * @param iiaXml XML containing IIA get response
   * @return list of hash comparison results (one for every IIA)
   * @throws ElementHashException when hash cannot be calculated
   */
  public List<HashComparisonResult> checkHash(InputSource iiaXml)
      throws ElementHashException, ParserConfigurationException, IOException, SAXException,
      XPathExpressionException, TransformerException {
    Document document = getDocument(iiaXml);

    NodeList iiasV6 = (NodeList) xpathIiasV6Expr.evaluate(document, XPathConstants.NODESET);
    if (iiasV6.getLength() > 0) {
      return getHashComparisonResultsV6(iiasV6);
    }

    NodeList iiasV7 = (NodeList) xpathIiasV7Expr.evaluate(document, XPathConstants.NODESET);
    if (iiasV7.getLength() > 0) {
      return getHashComparisonResultsV7(document, iiasV7);
    }

    return Collections.emptyList();
  }

  private List<HashComparisonResult> getHashComparisonResultsV7(Document document, NodeList iiasV7)
      throws IOException, TransformerException, ParserConfigurationException, SAXException,
      XPathExpressionException {

    Document result;
    try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      TransformerFactory transformerFactory = TransformerFactory.newInstance();
      Transformer transformer = transformerFactory.newTransformer(
          new StreamSource(IiaHashService.class.getResourceAsStream("/transform_version_7.xsl")));

      transformer.transform(new DOMSource(document), new StreamResult(output));

      result = getDocument(new InputSource(new ByteArrayInputStream(output.toByteArray())));
    }

    Map<String, String> idHashMap = new HashMap<>();
    for (int i = 0; i < iiasV7.getLength(); i++) {
      Node iia = iiasV7.item(i);
      idHashMap.put(xpathIiaIdExpr.evaluate(iia), xpathIiaHashExpr.evaluate(iia));
    }

    NodeList resultIias = result.getElementsByTagName("iia");
    List<HashComparisonResult> hashComparisonResults = new ArrayList<>(resultIias.getLength());
    for (int i = 0; i < resultIias.getLength(); i++) {
      hashComparisonResults.add(getV7HashComparisonResult(resultIias.item(i), idHashMap));
    }
    return hashComparisonResults;
  }

  private List<HashComparisonResult> getHashComparisonResultsV6(NodeList iiasV6)
      throws XPathExpressionException, ElementHashException {
    List<HashComparisonResult> hashComparisonResults;
    hashComparisonResults = new ArrayList<>(iiasV6.getLength());
    for (int i = 0; i < iiasV6.getLength(); i++) {
      hashComparisonResults.add(getV6HashComparisonResult(iiasV6.item(i)));
    }
    return hashComparisonResults;
  }

  private HashComparisonResult getV6HashComparisonResult(Node iia)
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

  private HashComparisonResult getV7HashComparisonResult(Node resultIia,
      Map<String, String> idHashMap) throws XPathExpressionException {
    String iiaId = xpathResultIiaIdExpr.evaluate(resultIia);
    String textToHash = xpathResultIiaTextToHashExpr.evaluate(resultIia);
    String hashExpected = DigestUtils.sha256Hex(textToHash);

    return new HashComparisonResult(idHashMap.get(iiaId), hashExpected, textToHash);
  }

  static class IiaNamespaceContext implements NamespaceContext {

    private final String iiasV6Ns;
    private final String iiasV7Ns;

    public IiaNamespaceContext(String iiasV6Ns, String iiasV7Ns) {
      this.iiasV6Ns = iiasV6Ns;
      this.iiasV7Ns = iiasV7Ns;
    }

    @Override
    public String getNamespaceURI(String prefix) {
      if ("iia6".equals(prefix)) {
        return iiasV6Ns;
      } else if ("iia7".equals(prefix)) {
        return iiasV7Ns;
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
