package eu.erasmuswithoutpaper.registry.documentbuilder;

import java.nio.charset.StandardCharsets;

/**
 * A set of parameters for the {@link EwpDocBuilder#build(BuildParams)} method.
 */
public class BuildParams {

  private final byte[] xml;
  private boolean pretty = false;
  private String expectedNamespaceUri = null;
  private String expectedLocalName = null;

  /**
   * Create an empty set of {@link BuildParams}.
   *
   * @param xml the XML contents to be parsed.
   */
  public BuildParams(byte[] xml) {
    this.xml = xml;
  }

  /**
   * Create an empty set of {@link BuildParams}.
   *
   * @param xml the XML contents to be parsed.
   */
  public BuildParams(String xml) {
    this(xml.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * Get the value set by {@link #setExpectedLocalName(String)}.
   *
   * @return see {@link #setExpectedLocalName(String)}.
   */
  public String getExpectedLocalName() {
    return this.expectedLocalName;
  }

  /**
   * Get the value set by {@link #setExpectedNamespaceUri(String)}.
   *
   * @return see {@link #setExpectedNamespaceUri(String)}.
   */
  public String getExpectedNamespaceUri() {
    return this.expectedNamespaceUri;
  }

  /**
   * @return XML contents to be parsed.
   */
  public byte[] getXml() {
    return this.xml;
  }

  /**
   * Get the value set by {@link #setMakingPretty(boolean)}.
   *
   * @return see {@link #setMakingPretty(boolean)}.
   */
  public boolean isMakingPretty() {
    return this.pretty;
  }

  /**
   * A shortcut for calling both {@link #setExpectedNamespaceUri(String)} and
   * {@link #setExpectedLocalName(String)}, if the expected element is among the
   * {@link KnownElement} enumeration.
   *
   * @param elem a {@link KnownElement} which you expect to find in the root of the parsed document.
   */
  public void setExpectedKnownElement(KnownElement elem) {
    this.setExpectedNamespaceUri(elem.getNamespaceUri());
    this.setExpectedLocalName(elem.getLocalName());
  }

  /**
   * Default is <b>null</b>. This behaves in the same way as
   * {@link #setExpectedNamespaceUri(String)} does, but it caused the element's local name to be
   * compared. (Usually you will want to set both.)
   *
   * @param expectedLocalName The new value for this flag.
   */
  public void setExpectedLocalName(String expectedLocalName) {
    this.expectedLocalName = expectedLocalName;
  }

  /**
   * Default is <b>null</b>. If you set it to a not-null value, then the result document's root
   * element's namespace URI will be compared against it. If it doesn't match, then
   * {@link BuildResult#isValid()} will return false, an additional {@link BuildError} will be
   * included in {@link BuildResult#getErrors()}.
   *
   * @param expectedNamespaceUri The new value for this flag.
   */
  public void setExpectedNamespaceUri(String expectedNamespaceUri) {
    this.expectedNamespaceUri = expectedNamespaceUri;
  }

  /**
   * Default is <b>false</b>. Set this to <b>true</b>, if you want the document to be converted to a
   * pretty-printed version before the validation begins.
   *
   * <p>
   * This influences error line numbers and some fields which will be filled in the returned
   * {@link BuildResult}. See {@link BuildResult} for details.
   * </p>
   *
   * @param pretty The new value for this flag.
   */
  public void setMakingPretty(boolean pretty) {
    this.pretty = pretty;
  }

}
