package eu.erasmuswithoutpaper.registry.common;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Optional;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import eu.erasmuswithoutpaper.registry.documentbuilder.KnownNamespace;

import org.springframework.web.util.HtmlUtils;

import org.apache.commons.lang.StringEscapeUtils;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * A set of common static utility methods.
 *
 * <p>
 * Some of these methods might seem redundant, but this is intentional. They serve as a facade to
 * make it easier to switch all calls from one library to another, for example from
 * {@link StringEscapeUtils#escapeHtml(String)} to {@link HtmlUtils#htmlEscape(String)}.
 * </p>
 */
public class Utils {

  private static final String[] ORDINAL_SUFFIXES =
      { "th", "st", "nd", "rd", "th", "th", "th", "th", "th", "th" };

  /**
   * Escape characters using HTML entities.
   *
   * @param str String to be escaped.
   * @return The escaped string.
   */
  public static String escapeHtml(String str) {
    return StringEscapeUtils.escapeHtml(str);
  }

  /**
   * Escape characters using XML entities.
   *
   * @param str String to be escaped.
   * @return The escaped string.
   */
  public static String escapeXml(String str) {
    return StringEscapeUtils.escapeXml(str);
  }

  /**
   * Get a new, safely configured instance of {@link DocumentBuilder}.
   *
   * @return a {@link DocumentBuilder} instance.
   */
  public static DocumentBuilder newSecureDocumentBuilder() {
    try {
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      dbf.setNamespaceAware(true);
      dbf.setIgnoringComments(true);

      /*
       * XXE prevention. See here:
       * https://www.owasp.org/index.php/XML_External_Entity_(XXE)_Prevention_Cheat_Sheet#Java
       */
      String feature = null;
      feature = "http://apache.org/xml/features/disallow-doctype-decl";
      dbf.setFeature(feature, true);
      feature = "http://xml.org/sax/features/external-general-entities";
      dbf.setFeature(feature, false);
      feature = "http://xml.org/sax/features/external-parameter-entities";
      dbf.setFeature(feature, false);
      feature = "http://apache.org/xml/features/nonvalidating/load-external-dtd";
      dbf.setFeature(feature, false);
      dbf.setXIncludeAware(false);
      dbf.setExpandEntityReferences(false);

      return dbf.newDocumentBuilder();
    } catch (ParserConfigurationException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Adds a proper suffix to a given integer, so that it can be used as an ordinal number.
   *
   * @param num e.g. 2 or 3
   * @return e.g. "2nd" or "3rd"
   */
  public static String ordinal(int num) {
    int modulo = num % 100;
    return num + ORDINAL_SUFFIXES[(modulo > 10 && modulo < 20) ? 0 : (modulo % 10)];
  }

  /**
   * Rewrite prefixes in the given DOM {@link Element} and all its descendants, so that they match
   * the preferred prefixes listed in {@link KnownNamespace} enumeration. Also remove all redundant
   * xmlns attributes.
   *
   * <p>
   * If an element's namespaceURI is not known (not listed in {@link KnownNamespace}), then its
   * prefix will be stripped instead. The resulting document will contain only the preferred
   * prefixes, and - possibly - lots of "xmlns" alterations.
   * </p>
   *
   * @param elem The element to apply the transformation to.
   */
  public static void rewritePrefixes(Element elem) {

    // Check if the element is using a known namespace.

    Optional<KnownNamespace> ns = KnownNamespace.findByNamespaceUri(elem.getNamespaceURI());
    String newPrefix;
    if (ns.isPresent()) {
      // Known namespace: Replace element's prefix with the preferred one.
      newPrefix = ns.get().getPreferredPrefix();
    } else {
      // Unknown namespace: Strip the existing prefix.
      newPrefix = null;
    }
    elem.setPrefix(newPrefix);

    /*
     * Remove all unnecessary "xmlns" and "xmlns:xxx" attributes. (We can remove *all* of them,
     * underlying DOM implementation will automatically re-add those which are needed.)
     */

    NamedNodeMap attrs = elem.getAttributes();
    for (int i = attrs.getLength() - 1; i >= 0; i--) {
      Attr attr = (Attr) attrs.item(i);
      if (attr.getNamespaceURI() != null
          && attr.getNamespaceURI().equals(XMLConstants.XMLNS_ATTRIBUTE_NS_URI)) {
        elem.removeAttributeNode(attr);
      }
    }

    // Repeat the action for child elements, recursively.

    NodeList childNodes = elem.getChildNodes();
    for (int i = 0; i < childNodes.getLength(); i++) {
      Node child = childNodes.item(i);
      if (child.getNodeType() == Node.ELEMENT_NODE) {
        rewritePrefixes((Element) child);
      }
    }
  }

  /**
   * Shortcut for {@link URLEncoder#encode(String, String)} method with UTF-8 charset.
   *
   * @param str String to be translated.
   * @return the translated String.
   */
  public static String urlencode(String str) {
    try {
      return URLEncoder.encode(str, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }
}
