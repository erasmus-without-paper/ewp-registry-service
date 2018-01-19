package eu.erasmuswithoutpaper.registry.common;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import eu.erasmuswithoutpaper.registry.documentbuilder.KnownNamespace;

import org.springframework.web.util.HtmlUtils;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.http.client.utils.DateUtils;
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
   * Convert a comma-separated string to a list of tokens. Tokens are trimmed, and empty ones are
   * skipped.
   *
   * @param value The comma-separated string to split.
   * @return The list of non-empty, trimmed tokens.
   */
  public static List<String> commaSeparatedTokens(String value) {
    if (value == null) {
      return new ArrayList<>();
    } else {
      return Arrays.asList(value.split(",")).stream().map(s -> s.trim()).filter(s -> s.length() > 0)
          .collect(Collectors.toList());
    }
  }


  /**
   * @param input Data to compute digest of.
   * @return Base64-encoded SHA-256 digest of the input.
   */
  public static String computeDigestBase64(byte[] input) {
    byte[] binaryDigest = DigestUtils.sha256(input);
    return Base64.getEncoder().encodeToString(binaryDigest);
  }

  /**
   * @param input Data to compute digest of.
   * @return Hex-encoded SHA-256 digest of the input.
   */
  public static String computeDigestHex(byte[] input) {
    return DigestUtils.sha256Hex(input);
  }

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
   * Extract all acceptable codings from the given value of the Accept-Encoding header.
   *
   * @param acceptEncodingHeader A string formatted in the same way as specified for the HTTP
   *        Accept-Encoding header.
   * @return The set of lowercase coding identifiers extracted. This will include "identity" unless
   *         it has been explicitly forbidden.
   */
  public static Set<String> extractAcceptableCodings(String acceptEncodingHeader) {
    Set<String> result = new LinkedHashSet<>();
    result.add("identity");
    boolean identityExplicitlyAdded = false;
    if (acceptEncodingHeader == null) {
      return result;
    }
    List<String> items = commaSeparatedTokens(acceptEncodingHeader).stream()
        .map(s -> s.toLowerCase()).collect(Collectors.toList());
    for (String entry : items) {
      List<String> params = Arrays.asList(entry.split(";")).stream().map(s -> s.trim())
          .filter(s -> s.length() > 0).collect(Collectors.toList());
      String coding = params.get(0);
      boolean acceptable = !params.contains("q=0");
      if (coding.equals("*")) {
        if ((!acceptable) && (!identityExplicitlyAdded)) {
          result.remove("identity");
        }
      } else {
        if (acceptable) {
          result.add(coding);
          if (coding.equals("identity")) {
            identityExplicitlyAdded = true;
          }
        } else {
          result.remove(coding);
        }
      }
    }
    return result;
  }

  /**
   * Validate the Date header for HTTP Signature usage.
   *
   * <p>
   * HTTP Signatures require the date to be in a valid RFC 2616 format, and to be no more than 5
   * minutes in the past or in the future. If the given date does not meet these criteria, then an
   * error message is returned.
   * </p>
   *
   * @param dateValue The value passed in the Date header.
   * @return Either {@link String} (the error message) or <code>null</code> (if no errors were
   *         found).
   */
  public static final String findErrorsInHttpSigDateHeader(String dateValue) {
    Date parsed = DateUtils.parseDate(dateValue);
    if (parsed == null) {
      return "Could not parse the date. Make sure it's in a valid RFC 2616 format.";
    }
    long given = parsed.getTime();
    long current = new Date().getTime();
    long differenceSec = Math.abs(current - given) / 1000;
    final long maxThresholdSec = 5 * 60;
    if (differenceSec > maxThresholdSec) {
      return "Server/client difference exceeds the maximum allowed threshold (it was "
          + differenceSec + " seconds; allowed: " + maxThresholdSec + ")";
    }
    // Seems valid.
    return null;
  }

  /**
   * Format a HTTP header name.
   *
   * @param headerName Header name, e.g. "content-type".
   * @return Pretty version, e.g. "Content-Type".
   */
  public static String formatHeaderName(String headerName) {
    return Arrays.asList(headerName.split("-", -1)).stream().map(s -> upperFirstLatter(s))
        .collect(Collectors.joining("-"));
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

  private static String upperFirstLatter(String word) {
    if (word.length() == 0) {
      return word;
    } else {
      return Character.toUpperCase(word.charAt(0)) + word.substring(1).toLowerCase(Locale.US);
    }
  }
}
