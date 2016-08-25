package eu.erasmuswithoutpaper.registry.xmlformatter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.xml.XMLConstants;

import org.springframework.stereotype.Service;

import com.google.common.base.Joiner;
import org.jdom2.Attribute;
import org.jdom2.Namespace;
import org.jdom2.input.DOMBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.Format.TextMode;
import org.jdom2.output.LineSeparator;
import org.jdom2.output.XMLOutputter;
import org.w3c.dom.Document;

/**
 * This service is responsible for pretty-printing XML documents in certain way.
 *
 * <p>
 * Apart from indenting elements, it will also sort and indent attributes (for any element which has
 * more than one of those). It will also add some additional whitespace to the xsi:schemaLocation
 * attribute (in order for the reader to easily determine which URI is a namespaceURI, and which is
 * its location).
 * </p>
 *
 * <p>
 * This formatter doesn't touch the namespace prefixes.
 * </p>
 */
@Service
public class XmlFormatter {

  /**
   * See {@link XmlFormatter} description.
   *
   * @param doc {@link org.w3c.dom.Document} to be formatted.
   * @return A string with the formatted XML.
   */
  public String format(Document doc) {

    /* Convert to JDOM document. */

    DOMBuilder domBuilder = new DOMBuilder();
    org.jdom2.Document jdoc = domBuilder.build(doc);
    return this.format(jdoc);
  }

  /**
   * See {@link XmlFormatter} description.
   *
   * @param doc {@link org.jdom2.Document} to be formatted.
   * @return A string with the formatted XML.
   */
  public String format(org.jdom2.Document doc) {

    /* Reformat the schemaLocation attribute. */

    Attribute attr = doc.getRootElement().getAttribute("schemaLocation",
        Namespace.getNamespace(XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI));
    if (attr != null) {
      String val = attr.getValue();
      List<String> inchunks = Arrays.asList(val.split("\\s+")).stream()
          .filter(str -> !str.isEmpty()).collect(Collectors.toList());
      List<String> outchunks = new ArrayList<String>(inchunks.size() / 2);
      if (inchunks.size() % 2 == 0) {
        for (int i = 0; i < inchunks.size(); i += 2) {
          outchunks.add(inchunks.get(i) + "\n        " + inchunks.get(i + 1));
        }
      }
      String newSchemaLocation =
          "\n        " + Joiner.on("\n\n        ").join(outchunks) + "\n    ";
      attr.setValue(newSchemaLocation);
    }

    /* Set up the formatter. */

    Format format = Format.getPrettyFormat();
    format.setLineSeparator(LineSeparator.NL);
    format.setIndent("    ");
    format.setOmitDeclaration(true);
    format.setTextMode(TextMode.TRIM_FULL_WHITE);

    XMLOutputter outputter = new XMLOutputter();
    outputter.setXMLOutputProcessor(new MyOutputProcessor());
    outputter.setFormat(format);

    /* Run it. */

    String xml = outputter.outputString(doc);
    return xml;
  }
}
