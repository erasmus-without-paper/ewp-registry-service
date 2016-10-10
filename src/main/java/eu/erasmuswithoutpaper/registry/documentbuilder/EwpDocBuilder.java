package eu.erasmuswithoutpaper.registry.documentbuilder;

import static org.joox.JOOX.$;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import eu.erasmuswithoutpaper.registry.common.Utils;
import eu.erasmuswithoutpaper.registry.xmlformatter.XmlFormatter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.xerces.util.XMLCatalogResolver;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Builds and validates EWP documents.
 */
@Service
@SuppressFBWarnings("SIC_INNER_SHOULD_BE_STATIC_ANON")
public class EwpDocBuilder {

  private final XmlFormatter xmlFormatter;
  private final Schema compoundSchema;

  /**
   * @param resLoader needed for loading XSDs from resources.
   * @param xmlFormatter needed for pretty-printing the validation results.
   */
  @Autowired
  public EwpDocBuilder(ResourceLoader resLoader, XmlFormatter xmlFormatter) {

    this.xmlFormatter = xmlFormatter;

    /*
     * 1. Prepare the XMLCatalogResolver.
     *
     * It should be able to resolve all known schema namespaceURIs to internal Resources kept in our
     * classpath.
     */

    Resource xmlCatalog = resLoader.getResource("classpath:schemas/__index__.xml");
    String catalogUrl;
    try {
      catalogUrl = xmlCatalog.getURL().toString();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    XMLCatalogResolver baseResolver = new XMLCatalogResolver(new String[] { catalogUrl });


    /*
     * 2. Wrap it in a custom LSResourceResolver.
     *
     * XMLCatalogResolver implements the LSResourceResolver interface, but we need it to behave
     * differently. We want to make sure that we have all necessary XSD files in our resources, so
     * that the compiler doesn't depend on the external XSDs dynamically fetched from the Internet.
     *
     * In order to assure that, we will use our custom resource resolver which will throw
     * RuntimeException whenever the compiler attempts to resolve resources which are NOT present in
     * our catalog (thus preventing it from trying to resolve them online).
     */

    LSResourceResolver customResolver = new LSResourceResolver() {
      @Override
      public LSInput resolveResource(String type, String namespaceUri, String publicId,
          String systemId, String baseUri) {

        // First, try to resolve the entity from our built-in schema catalog.

        LSInput result =
            baseResolver.resolveResource(type, namespaceUri, publicId, systemId, baseUri);
        if (result != null) {
          return result;
        }

        /*
         * Should not happen. It is does, then it means that some of our schemas reference other
         * schemas which are not present in our schema catalog. The catalog needs to be updated.
         */

        throw new RuntimeException("Missing schema in resources: " + namespaceUri);
      }
    };

    /* 3. Create the SchemaFactory. Assign our custom resolver. */

    SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
    schemaFactory.setResourceResolver(customResolver);

    /* 4. Get a list of all StreamSources with all our schemas. */

    List<StreamSource> xsdSources = new ArrayList<>();
    try {
      for (Element element : $(xmlCatalog.getInputStream()).find("uri")) {
        String relativePath = $(element).attr("uri");
        Resource xsd = resLoader.getResource("classpath:schemas/" + relativePath);
        StreamSource xsdSource = new StreamSource(xsd.getInputStream());
        xsdSources.add(xsdSource);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    } catch (SAXException e) {
      throw new RuntimeException(e);
    }

    /* 5. Generate a compound schema. */

    try {
      this.compoundSchema =
          schemaFactory.newSchema(xsdSources.toArray(new StreamSource[xsdSources.size()]));
    } catch (SAXException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Parse, build and validate an EWP document.
   *
   * <p>
   * This method uses built-in set of XML Schemas to validate documents. It can also be used to
   * pretty-print the validation results.
   * </p>
   *
   * @param input Required set of input parameters (including the XML content).
   * @return An object describing the results of the validation.
   */
  public BuildResult build(BuildParams input) {

    byte[] xml = input.getXml();

    // We will need to load the document first. This is not required for validation, but it is
    // required for some validation result fields.

    DocumentBuilder docBuilder = Utils.newSecureDocumentBuilder();
    Document doc;
    try {
      doc = docBuilder.parse(new ByteArrayInputStream(xml));
    } catch (SAXException e) {
      String recovered = null;
      List<String> notSoPrettyLines = null;
      if (input.isMakingPretty()) {
        recovered = new String(xml, StandardCharsets.UTF_8);
        notSoPrettyLines = Lists.newArrayList(Splitter.on("\n").split(recovered));
      }
      List<BuildError> parseErrors = new ArrayList<>();
      parseErrors.add(new BuildError(e.getMessage()));
      return new BuildResult(false, null, null, null, parseErrors, recovered, notSoPrettyLines);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    // Do we need pretty output?

    String prettyXml = null;
    List<String> prettyLines = null;
    if (input.isMakingPretty()) {
      prettyXml = this.xmlFormatter.format(doc);
      xml = prettyXml.getBytes(StandardCharsets.UTF_8);
      prettyLines = Lists.newArrayList(Splitter.on("\n").split(prettyXml));
    }

    // Validators are not thread-safe (hence, separate instance).

    Validator validator = this.compoundSchema.newValidator();

    // We will store all errors here.

    List<BuildError> buildErrors = new ArrayList<>();
    validator.setErrorHandler(new ErrorHandler() {

      @Override
      public void error(SAXParseException exception) throws SAXException {
        buildErrors.add(new BuildError(exception));
      }

      @Override
      public void fatalError(SAXParseException exception) throws SAXException {
        buildErrors.add(new BuildError(exception));
      }

      @Override
      public void warning(SAXParseException exception) throws SAXException {
        buildErrors.add(new BuildError(exception));
      }
    });

    // Run the validator.

    try {
      validator.validate(new StreamSource(new ByteArrayInputStream(xml)));
    } catch (IOException | SAXException e) {
      throw new RuntimeException(e);
    }

    // Compose a result.

    boolean isValid = buildErrors.isEmpty();
    String rootNamespaceUri = doc.getDocumentElement().getNamespaceURI();
    String rootLocalName = doc.getDocumentElement().getLocalName();

    return new BuildResult(isValid, doc, rootNamespaceUri, rootLocalName, buildErrors, prettyXml,
        prettyLines);
  }
}
