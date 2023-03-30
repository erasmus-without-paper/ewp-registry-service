package eu.erasmuswithoutpaper.registry.updater;

import java.util.List;
import java.util.stream.Collectors;

import eu.erasmuswithoutpaper.registry.documentbuilder.BuildError;
import eu.erasmuswithoutpaper.registry.documentbuilder.BuildParams;
import eu.erasmuswithoutpaper.registry.documentbuilder.BuildResult;
import eu.erasmuswithoutpaper.registry.documentbuilder.EwpDocBuilder;
import eu.erasmuswithoutpaper.registry.documentbuilder.KnownElement;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * This service provides a way to parse manifests in supported versions.
 */
@Service
public class ManifestParser {

  /**
   * Thrown when a valid manifest document was expected (in any non-discontinued Discovery API
   * version), but the provided document was not a valid manifest.
   */
  @SuppressWarnings("serial")
  public abstract static class NotValidManifest extends Exception {

    /**
     * @return The list of error messages telling you what was wrong about the (presumed) manifest.
     */
    public abstract List<String> getErrorList();
  }

  @SuppressWarnings("serial")
  @SuppressFBWarnings("SE_BAD_FIELD")
  private static class NotValidEwpDocument extends NotValidManifest {

    private final BuildResult buildResult;

    private NotValidEwpDocument(BuildResult buildResult) {
      this.buildResult = buildResult;
    }

    /**
     * @return {@link BuildResult} returned by the internal {@link EwpDocBuilder}. Note, that it MAY
     *     be valid, but the parsed document might not be a manifest at all.
     */
    @Override
    public List<String> getErrorList() {
      return this.buildResult.getErrors().stream().map(Object::toString)
          .collect(Collectors.toList());
    }
  }

  @SuppressWarnings("serial")
  private static class ValidEwpDocumentButNotManifest extends NotValidManifest {

    private final String namespaceUri;
    private final String localName;

    private ValidEwpDocumentButNotManifest(Element root) {
      this.namespaceUri = root.getNamespaceURI();
      this.localName = root.getLocalName();
    }

    @Override
    public List<String> getErrorList() {
      return Lists.newArrayList("This is a valid {" + this.namespaceUri + "}" + this.localName
          + " document, but we are expecting a manifest.");
    }
  }

  private final EwpDocBuilder docBuilder;

  /**
   * @param docBuilder
   *     Needed to run XML schema validation on the provided manifest documents.
   */
  @Autowired
  public ManifestParser(EwpDocBuilder docBuilder) {
    this.docBuilder = docBuilder;
  }

  /**
   * Parse XML manifest in ANY supported version.
   *
   * @param manifestXmlContents
   *     XML contents of the presumed manifest file.
   * @param nonLethalErrors
   *     List that will be filled with found non-lethal errors.
   * @return A DOM {@link Document} with Manifest.
   * @throws NotValidManifest
   *     If the provided document was not a valid Manifest (in any of its
   *     supported versions).
   */
  public Document parseManifest(byte[] manifestXmlContents,
      List<BuildError> nonLethalErrors) throws NotValidManifest {

    // Try to parse it.

    BuildParams params = new BuildParams(manifestXmlContents);
    BuildResult result = this.docBuilder.buildManifest(params);

    if (!result.isValid()) {
      throw new NotValidEwpDocument(result);
    }

    if (nonLethalErrors != null) {
      nonLethalErrors.addAll(result.getErrors());
    }

    // Verify the namespace.

    Document doc = result.getDocument().get();
    if (KnownElement.RESPONSE_MANIFEST_V6.matches(doc.getDocumentElement())) {
      return doc;
    } else {
      // Despite the document being valid in itself, it's not a manifest at all.
      // (For example, it can be a valid Registry response document.)
      throw new ValidEwpDocumentButNotManifest(doc.getDocumentElement());
    }
  }

}
