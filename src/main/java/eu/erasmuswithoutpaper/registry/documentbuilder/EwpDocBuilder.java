package eu.erasmuswithoutpaper.registry.documentbuilder;

import static org.joox.JOOX.$;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.sax.SAXSource;
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
import https.github_com.erasmus_without_paper.ewp_specs_api_discovery.tree.stable_v6.Manifest;
import org.apache.xerces.util.XMLCatalogResolver;
import org.joox.Match;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.Attributes;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLFilterImpl;

/**
 * Builds and validates EWP documents.
 */
@Service
@SuppressFBWarnings("SIC_INNER_SHOULD_BE_STATIC_ANON")
public class EwpDocBuilder {

  private final XmlFormatter xmlFormatter;
  private final Schema compoundSchema;
  private static final Logger logger = LoggerFactory.getLogger(EwpDocBuilder.class);

  /**
   * @param resLoader
   *     needed for loading XSDs from resources.
   * @param xmlFormatter
   *     needed for pretty-printing the validation results.
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
    XMLCatalogResolver baseResolver = new XMLCatalogResolver(new String[]{catalogUrl});


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

        throw new RuntimeException("Missing schema in registry's resources:\nnamespaceUri: "
            + namespaceUri + "\ntype: " + type + "\npublicId: " + publicId + "\nsystemId: "
            + systemId + "\nbaseUri: " + baseUri);
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
    } catch (IOException | SAXException e) {
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
   * @param input
   *     Required set of input parameters (including the XML content).
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

    // Check if the root element matches requirements.

    String rootNamespaceUri = doc.getDocumentElement().getNamespaceURI();
    String rootLocalName = doc.getDocumentElement().getLocalName();
    if (input.getExpectedLocalName() != null
        && (!rootLocalName.equals(input.getExpectedLocalName()))) {
      buildErrors.add(new BuildError("Expecting \"" + input.getExpectedLocalName()
          + "\" element, but found \"" + rootLocalName + "\" element instead."));
    }
    if (input.getExpectedNamespaceUri() != null) {
      if (rootNamespaceUri == null) {
        buildErrors
            .add(new BuildError("Expecting element from the \"" + input.getExpectedNamespaceUri()
                + "\" namespace, but found an element " + "without any namespace instead."));
      } else if (!rootNamespaceUri.equals(input.getExpectedNamespaceUri())) {
        buildErrors.add(new BuildError("Expecting element from the \""
            + input.getExpectedNamespaceUri() + "\" namespace, but found an element from \""
            + rootNamespaceUri + "\" namespace instead."));
      }
    }

    // Compose a result.

    boolean isValid = buildErrors.isEmpty();

    return new BuildResult(isValid, doc, rootNamespaceUri, rootLocalName, buildErrors, prettyXml,
        prettyLines);
  }

  /**
   * Parse, build and validate an EWP manifest.
   *
   * <p>
   * This method uses built-in set of XML Schemas to validate manifest files.
   * Schema validation errors in XML elements defining APIs are treated as warnings,
   * invalid entries are removed from the result and won't be included in the Catalogue.
   * </p>
   *
   * @param input
   *     Required set of input parameters (including the XML content).
   * @return An object describing the results of the validation.
   */
  public BuildResult buildManifest(BuildParams input) {

    byte[] xml = input.getXml();

    // We will need to load the document first. This checks if it is syntactically correct.

    DocumentBuilder docBuilder = Utils.newSecureDocumentBuilder();
    Document doc;
    Document docToModify;
    try {
      doc = docBuilder.parse(new ByteArrayInputStream(xml));
      docToModify = docBuilder.parse(new ByteArrayInputStream(xml));
    } catch (SAXException e) {
      List<BuildError> parseErrors = new ArrayList<>();
      parseErrors.add(new BuildError(e.getMessage()));
      return new BuildResult(false, null, null, null, parseErrors);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    String rootNamespaceUri = doc.getDocumentElement().getNamespaceURI();
    String rootLocalName = doc.getDocumentElement().getLocalName();

    IgnoreApisValidationEventHandler eventHandler;
    Unmarshaller unmarshaller;
    SAXSource source;

    try {
      SAXParserFactory spf = SAXParserFactory.newInstance();
      spf.setNamespaceAware(true);
      XMLReader xmlReader = spf.newSAXParser().getXMLReader();

      // We use custom XmlFilter to keep track of elements visited by JAXB unmarshaller.
      RecordingXmlFilter xmlFilter = new RecordingXmlFilter(xmlReader);

      // And custom event handler to handle validation errors inside api elements differently.
      eventHandler = new IgnoreApisValidationEventHandler(xmlFilter);
      source = new SAXSource(xmlFilter, new InputSource(new ByteArrayInputStream(xml)));

      JAXBContext jc = JAXBContext.newInstance(Manifest.class);
      unmarshaller = jc.createUnmarshaller();
      unmarshaller.setSchema(this.compoundSchema);
      unmarshaller.setEventHandler(eventHandler);
    } catch (JAXBException | ParserConfigurationException | SAXException e) {
      return new BuildResult(false, doc, rootNamespaceUri, rootLocalName,
          Collections.singletonList(new BuildError("Internal error: " + e.getMessage())));
    }

    try {
      // Ignoring the result of unmarshalling, we just care if it throws an exception what means
      // that xml is invalid.
      unmarshaller.unmarshal(source);
    } catch (JAXBException e) {
      return new BuildResult(false, doc, rootNamespaceUri, rootLocalName, eventHandler.errors);
    }

    // We passed the validation, any incorrect API entries are stored by eventHandler

    // Remove all invalid APIs
    for (RecordingXmlFilter.XmlPathWithUri xmlElementPath : eventHandler.invalidApis) {
      Match match = xmlElementPath.selectFromDocument(docToModify);
      if (match.size() == 1) {
        match.get(0).getParentNode().removeChild(match.get(0));
      } else {
        // This shouldn't happen, but if it does then just report that the manifest is invalid.
        logger.error("There was an unexpected failure during validation of manifest,"
            + " we have generated wrong selector that has selected != 1 api entries."
            + " Invalid selector:\n" + xmlElementPath.toString());
        return new BuildResult(false, doc, rootNamespaceUri, rootLocalName,
            eventHandler.invalidApisErrors);
      }
    }

    return new BuildResult(true, docToModify, rootNamespaceUri, rootLocalName,
        eventHandler.invalidApisErrors);
  }

  private static final class IgnoreApisValidationEventHandler implements ValidationEventHandler {
    private final RecordingXmlFilter xmlFilter;
    private final List<BuildError> errors = new ArrayList<>();
    private final Set<RecordingXmlFilter.XmlPathWithUri> invalidApis = new HashSet<>();
    private final List<BuildError> invalidApisErrors = new ArrayList<>();

    public IgnoreApisValidationEventHandler(RecordingXmlFilter xmlFilter) {
      this.xmlFilter = xmlFilter;
    }

    @Override
    public boolean handleEvent(ValidationEvent validationEvent) {
      if (xmlFilter.isCurrentlyInsideApi()) {
        invalidApis.add(xmlFilter.getApiPath());
        invalidApisErrors.add(new BuildError(validationEvent));
        return true; // ignore this error and continue validation
      }
      errors.add(new BuildError(validationEvent));
      return false;
    }
  }

  private static final class RecordingXmlFilter extends XMLFilterImpl {
    public static final class XmlElementWithUri {
      public String uri;
      public String name;

      @Override
      public String toString() {
        return "{" + this.uri + "}:" + this.name;
      }

      public XmlElementWithUri(String uri, String name) {
        this.uri = uri;
        this.name = name;
      }

      @Override
      public boolean equals(Object other) {
        if (this == other) {
          return true;
        }
        if (other == null || getClass() != other.getClass()) {
          return false;
        }
        XmlElementWithUri that = (XmlElementWithUri) other;
        return Objects.equals(uri, that.uri) && Objects.equals(name, that.name);
      }

      @Override
      public int hashCode() {
        return Objects.hash(uri, name);
      }
    }

    public static final class XmlPathWithUri {
      List<XmlElementWithUri> path;

      @Override
      public String toString() {
        return path.stream().map(XmlElementWithUri::toString).collect(Collectors.joining("/"));
      }

      public XmlPathWithUri(Collection<XmlElementWithUri> path) {
        this.path = new ArrayList<>(path);
      }

      public Match selectFromDocument(Document doc) {
        StringBuilder selector = new StringBuilder();
        Map<String, String> prefixes = new HashMap<>();
        int prefixId = 0;
        for (RecordingXmlFilter.XmlElementWithUri xmlElementWithUri : this.path) {
          String prefix = "p" + prefixId++;
          prefixes.put(prefix, xmlElementWithUri.uri);
          selector.append('/');
          selector.append(prefix);
          selector.append(':');
          selector.append(xmlElementWithUri.name);
        }
        return $(doc.getDocumentElement()).namespaces(prefixes).xpath(selector.toString());
      }

      @Override
      public boolean equals(Object other) {
        if (this == other) {
          return true;
        }
        if (other == null || getClass() != other.getClass()) {
          return false;
        }
        XmlPathWithUri that = (XmlPathWithUri) other;
        return Objects.equals(path, that.path);
      }

      @Override
      public int hashCode() {
        return Objects.hash(path);
      }
    }

    Stack<XmlElementWithUri> currentPath = new Stack<>();

    private RecordingXmlFilter(XMLReader parent) {
      super(parent);
    }

    @Override
    public void startElement(String uri, String localName, String qualifiedName,
        Attributes attributes) throws SAXException {
      currentPath.push(new XmlElementWithUri(uri, localName));
      super.startElement(uri, localName, qualifiedName, attributes);
    }

    @Override
    public void endElement(String uri, String localName, String qualifiedName)
        throws SAXException {
      currentPath.pop();
      super.endElement(uri, localName, qualifiedName);
    }

    private int getApisImplementedIndex() {
      for (int i = 0; i < this.currentPath.size(); i++) {
        if (this.currentPath.get(i).name.equals("apis-implemented")) {
          return i;
        }
      }
      return -1;
    }

    public boolean isCurrentlyInsideApi() {
      int apisImplementedIndex = getApisImplementedIndex();
      return 0 <= apisImplementedIndex && apisImplementedIndex < this.currentPath.size() - 1;
    }

    public XmlPathWithUri getApiPath() {
      int apisImplementedIndex = getApisImplementedIndex();

      if (apisImplementedIndex == -1) {
        return new XmlPathWithUri(new ArrayList<>());
      }

      List<XmlElementWithUri> result = new ArrayList<>();
      for (int i = 0; i <= apisImplementedIndex + 1; i++) {
        result.add(this.currentPath.get(i));
      }
      return new XmlPathWithUri(result);
    }
  }
}
