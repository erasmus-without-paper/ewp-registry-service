package eu.erasmuswithoutpaper.registry.iia;

import java.io.IOException;
import java.util.Iterator;

import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.springframework.stereotype.Service;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

@Service
public class CooperationService {

  static final String IIAS_NS =
      "https://github.com/erasmus-without-paper/ewp-specs-api-iias/blob/stable-v6/endpoints/get-response.xsd";
  private final XPathExpression xpathCooperationConditionsHashExpr;
  private final XPathExpression xpathCooperationConditionsExpr;

  CooperationService() throws XPathExpressionException {
    XPathFactory xpathFactory = XPathFactory.newInstance();
    XPath xpath = xpathFactory.newXPath();
    xpath.setNamespaceContext(new IiaNamespaceContext());

    xpathCooperationConditionsHashExpr =
        xpath.compile("/iia:iias-get-response/iia:iia/iia:conditions-hash/text()");
    xpathCooperationConditionsExpr =
        xpath.compile("/iia:iias-get-response/iia:iia/iia:cooperation-conditions");
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
      NodeList sendingContacts = mobilitySpec.getElementsByTagNameNS(IIAS_NS, "sending-contact");
      for (int j = sendingContacts.getLength() - 1; j >= 0; j--) {
        mobilitySpec.removeChild(sendingContacts.item(j));
      }

      NodeList receivingContacts =
          mobilitySpec.getElementsByTagNameNS(IIAS_NS, "receiving-contact");
      for (int j = receivingContacts.getLength() - 1; j >= 0; j--) {
        mobilitySpec.removeChild(receivingContacts.item(j));
      }
    }
  }

  /**
   * Checks cooperation conditions hash present in IIA get response.
   *
   * @param iiaXml XML containing IIA get response
   * @return cooperation conditions hash comparison result
   * @throws ElementHashException when hash cannot be calculated
   */
  public HashComparisonResult checkCooperationConditionsHash(InputSource iiaXml)
      throws ElementHashException, ParserConfigurationException, IOException, SAXException,
      XPathExpressionException {
    Document document = getDocument(iiaXml);

    String hashExtracted = xpathCooperationConditionsHashExpr.evaluate(document);
    Node cooperationConditions =
        (Node) xpathCooperationConditionsExpr.evaluate(document, XPathConstants.NODE);
    removeContacts(cooperationConditions);
    String hashExpected = ElementHashHelper.getXmlHash(cooperationConditions);

    return new HashComparisonResult(hashExtracted, hashExpected);
  }

  static class IiaNamespaceContext implements NamespaceContext {
    @Override
    public String getNamespaceURI(String prefix) {
      if ("iia".equals(prefix)) {
        return IIAS_NS;
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
