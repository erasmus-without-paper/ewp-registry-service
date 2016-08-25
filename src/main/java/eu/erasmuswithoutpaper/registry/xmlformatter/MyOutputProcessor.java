package eu.erasmuswithoutpaper.registry.xmlformatter;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

import com.google.common.collect.Iterables;
import org.jdom2.Attribute;
import org.jdom2.Content;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.output.Format;
import org.jdom2.output.support.AbstractXMLOutputProcessor;
import org.jdom2.output.support.FormatStack;
import org.jdom2.output.support.Walker;
import org.jdom2.util.NamespaceStack;


/**
 * A customized subclass of JDOM's {@link AbstractXMLOutputProcessor}. Among other things, it forces
 * the attributes into separate lines.
 *
 * <p>
 * Most of this code (including some comments) has been <em>copied</em> from
 * {@link AbstractXMLOutputProcessor}, because this parent didn't allow us to override properties we
 * needed overridden. As a result, it might be required for you to diff specific portions of this
 * code and the base implementation in order to determine what was changed exactly.
 * </p>
 */
public class MyOutputProcessor extends AbstractXMLOutputProcessor {

  /**
   * This replaces the original {@link AbstractXMLOutputProcessor#attributeEscapedEntitiesFilter} to
   * allow the use of newlines inside the attributes of the top level element.
   *
   * <p>
   * The original implementation replaced them with <code>&amp;#xA;</code> which - in general - is a
   * proper way to handle them. But in our formatter we want to pretty print the long lists of
   * <code>xsi:schemaLocation</code> attributes, so we need to handle this a bit differently.
   * </p>
   *
   * <p>
   * Note, that this is still a perfectly valid XML syntax. (As per XML specs, XML parsers are
   * required to map such whitespace to regular spaces.)
   * </p>
   */
  @Override
  protected void attributeEscapedEntitiesFilter(Writer out, FormatStack fstack, String value)
      throws IOException {

    if (!fstack.getEscapeOutput()) {
      // no escaping...
      this.write(out, value);
      return;
    }

    String escaped = Format.escapeAttribute(fstack.getEscapeStrategy(), value);
    if (fstack.getLevelIndent() == null || fstack.getLevelIndent().length() == 0) {
      // Top level element. We want xsi:schemaLocation to contain newlines.
      escaped = escaped.replace("&#xA;", "\n");
    }
    this.write(out, escaped);
  }

  /**
   * Modified version of {@link #printAttribute(Writer, FormatStack, Attribute)}.
   *
   * <p>
   * This handles the printing of an {@link Attribute} in a different way. Each attribute is put
   * into a separate line, with an appropriate indent.
   * </p>
   *
   * @param out <code>Writer</code> to use.
   * @param fstack The current FormatStack
   * @param attribute <code>Attribute</code> to output
   * @throws IOException if the output fails
   */
  protected void printAttribute2(final Writer out, final FormatStack fstack,
      final Attribute attribute) throws IOException {

    if (!attribute.isSpecified() && fstack.isSpecifiedAttributesOnly()) {
      return;
    }
    this.write(out, "\n");
    this.write(out, fstack.getLevelIndent());
    this.write(out, fstack.getIndent()); // + one more
    this.write(out, attribute.getQualifiedName());
    this.write(out, "=");

    this.write(out, "\"");
    this.attributeEscapedEntitiesFilter(out, fstack, attribute.getValue());
    this.write(out, "\"");
  }

  @Override
  protected void printElement(final Writer out, final FormatStack fstack,
      final NamespaceStack nstack, final Element element) throws IOException {

    nstack.push(element);
    try {
      final List<Content> content = element.getContent();
      this.write(out, "<");
      this.write(out, element.getQualifiedName());

      final int totalAttrCount =
          Iterables.size(nstack.addedForward()) + element.getAttributes().size();

      for (final Namespace ns : nstack.addedForward()) {
        if (totalAttrCount >= 2) {
          this.printNamespace2(out, fstack, ns);
        } else {
          this.printNamespace(out, fstack, ns);
        }
      }
      if (element.hasAttributes()) {

        if (totalAttrCount >= 2) {
          for (final Attribute attribute : element.getAttributes()) {
            this.printAttribute2(out, fstack, attribute);
          }
        } else {
          for (final Attribute attribute : element.getAttributes()) {
            this.printAttribute(out, fstack, attribute);
          }
        }
      }
      if (totalAttrCount >= 2) {
        out.write("\n");
        if (fstack.getLevelIndent() != null) {
          out.write(fstack.getLevelIndent());
        }
      }
      if (content.isEmpty()) {
        if (fstack.isExpandEmptyElements()) {
          this.write(out, "></");
          this.write(out, element.getQualifiedName());
          this.write(out, ">");
        } else {
          this.write(out, "/>");
        }
        // nothing more to do.
        return;
      }

      // OK, we have real content to push.
      fstack.push();
      try {

        // note we ensure the FStack is right before creating the walker
        Walker walker = this.buildWalker(fstack, content, true);

        if (!walker.hasNext()) {
          // the walker has formatted out whatever content we had
          if (fstack.isExpandEmptyElements()) {
            this.write(out, "></");
            this.write(out, element.getQualifiedName());
            this.write(out, ">");
          } else {
            this.write(out, "/>");
          }
          // nothing more to do.
          return;
        }
        // we have some content.
        this.write(out, ">");
        if (!walker.isAllText()) {
          // we need to newline/indent
          this.textRaw(out, fstack.getPadBetween());
        }

        this.printContent(out, fstack, nstack, walker);

        if (!walker.isAllText()) {
          // we need to newline/indent
          this.textRaw(out, fstack.getPadLast());
        }
        this.write(out, "</");
        this.write(out, element.getQualifiedName());
        this.write(out, ">");

      } finally {
        fstack.pop();
      }
    } finally {
      nstack.pop();
    }

  }

  /**
   * Modified version of {@link #printNamespace(Writer, FormatStack, Namespace)}.
   *
   * <p>
   * This handles printing of {@link Namespace} declarations in the same way as
   * {@link #printAttribute2(Writer, FormatStack, Attribute)} does for {@link Attribute}s (one per
   * line, with proper indent).
   * </p>
   *
   * @param out <code>Writer</code> to use.
   * @param fstack The current FormatStack
   * @param ns <code>Namespace</code> to print definition of
   * @throws IOException if the output fails
   */
  protected void printNamespace2(final Writer out, final FormatStack fstack, final Namespace ns)
      throws IOException {
    final String prefix = ns.getPrefix();
    final String uri = ns.getURI();

    this.write(out, "\n");
    this.write(out, fstack.getLevelIndent());
    this.write(out, fstack.getIndent()); // + one more
    this.write(out, "xmlns");
    if (!prefix.isEmpty()) {
      this.write(out, ":");
      this.write(out, prefix);
    }
    this.write(out, "=\"");
    this.attributeEscapedEntitiesFilter(out, fstack, uri);
    this.write(out, "\"");
  }
}
