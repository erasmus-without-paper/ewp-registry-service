package eu.erasmuswithoutpaper.registry.documentbuilder;

import java.util.List;
import java.util.Optional;

import org.w3c.dom.Document;

/**
 * This object describes the results of {@link EwpDocBuilder#build(BuildParams)} parsing and
 * validation process.
 */
public class BuildResult {

  private final boolean valid;
  private final Optional<Document> document;
  private final String rootNamespaceUri;
  private final String rootLocalName;
  private final List<BuildError> buildErrors;
  private final Optional<String> prettyXml;
  private final Optional<List<String>> prettyLines;

  BuildResult(boolean isValid, Document document, String rootNamespaceUri, String rootLocalName,
      List<BuildError> buildErrors) {
    this.valid = isValid;
    this.document = Optional.ofNullable(document);
    this.rootNamespaceUri = rootNamespaceUri;
    this.rootLocalName = rootLocalName;
    this.buildErrors = buildErrors;
    this.prettyXml = Optional.empty();
    this.prettyLines = Optional.empty();
  }

  BuildResult(boolean isValid, Document document, String rootNamespaceUri, String rootLocalName,
      List<BuildError> buildErrors, String prettyXml, List<String> prettyLines) {
    this.valid = isValid;
    this.document = Optional.ofNullable(document);
    this.rootNamespaceUri = rootNamespaceUri;
    this.rootLocalName = rootLocalName;
    this.buildErrors = buildErrors;
    this.prettyXml = Optional.ofNullable(prettyXml);
    this.prettyLines = Optional.ofNullable(prettyLines);
  }

  /**
   * Retrieve the parsed {@link Document}.
   *
   * @return an {@link Optional} with the parsed document. It will be empty if the XML could not be
   *         parsed.
   */
  public Optional<Document> getDocument() {
    return this.document;
  }

  /**
   * @return The list {@link BuildError}s encountered during the parsing and validation. This list
   *         will be empty whenever {@link #isValid()} is <b>true</b>.
   */
  public List<BuildError> getErrors() {
    return this.buildErrors;
  }

  /**
   * If pretty-printing was requested in {@link BuildParams#setMakingPretty(boolean)}, then this
   * parameter will contain the <b>list of lines</b> with the results of the pretty printing.
   *
   * <p>
   * The value will be returned <i>even if the document has failed parsing</i>. However, it will not
   * be so pretty in this case.
   * </p>
   *
   * <p>
   * Note, that this list is 0-based, whereas {@link BuildError#getLineNumber()} is not.
   * </p>
   *
   * @return An {@link Optional} with the value. It will be present if
   *         {@link BuildParams#setMakingPretty(boolean)} was set to <b>true</b>.
   */
  public Optional<List<String>> getPrettyLines() {
    return this.prettyLines;
  }

  /**
   * If pretty-printing was requested in {@link BuildParams#setMakingPretty(boolean)}, then this
   * parameter will contain the results of the pretty printing.
   *
   * <p>
   * The value will be returned <i>even if the document has failed parsing</i>. However, it will not
   * be so pretty in this case.
   * </p>
   *
   * @return An {@link Optional} with the value. It will be present if
   *         {@link BuildParams#setMakingPretty(boolean)} was set to <b>true</b>.
   */
  public Optional<String> getPrettyXml() {
    return this.prettyXml;
  }

  /**
   * @return local name of the parsed document root, or <b>null</b>, if the document could not be
   *         parsed.
   */
  public String getRootLocalName() {
    return this.rootLocalName;
  }

  /**
   * Try to retrieve the namespace URI of the document root.
   *
   * <p>
   * Please note, that this method will return <b>null</b> in two very different cases:
   * </p>
   *
   * <ul>
   * <li>if the document could not be parsed, or</li>
   * <li>if the root element doesn't have any namespace.</li>
   * </ul>
   *
   * @return String or null, as described above.
   */
  public String getRootNamespaceUri() {
    return this.rootNamespaceUri;
  }

  /**
   * Boolean result of the parsing and validation.
   *
   * <p>
   * The result depends on the additional conditions indicated in {@link BuildParams} provided when
   * calling {@link EwpDocBuilder#build(BuildParams)} method.
   * </p>
   *
   * @return <b>true</b> if the document was parsed, validated, and all other conditions were met.
   */
  public boolean isValid() {
    return this.valid;
  }
}
