package eu.erasmuswithoutpaper.registry.documentbuilder;

import org.w3c.dom.Element;

/**
 * A collection of primary EWP element definitions, handled by the Registry Service.
 *
 * <p>
 * This enumeration is just a convenience created especially for the purpose of being called with
 * {@link BuildParams#setExpectedKnownElement(KnownElement)} method. It doesn't serve any other real
 * purpose.
 * </p>
 */
public class KnownElement {

  /**
   * The root of the Discovery API v4 response.
   */
  public static final KnownElement RESPONSE_MANIFEST_V4 =
      new KnownElement(KnownNamespace.RESPONSE_MANIFEST_V4, "manifest", "Discovery Manifest file");


  /**
   * The root of the Registry API v1 catalogue response.
   */
  public static final KnownElement RESPONSE_REGISTRY_V1_CATALOGUE = new KnownElement(
      KnownNamespace.RESPONSE_REGISTRY_V1, "catalogue", "Registry Service <catalogue> response");

  /**
   * The root of the common &lt;error-response/&gt;, as defined in the
   * <a href='https://github.com/erasmus-without-paper/ewp-specs-architecture'>EWP Architecture
   * document</a>.
   */
  public static final KnownElement COMMON_ERROR_RESPONSE =
      new KnownElement(KnownNamespace.COMMON_TYPES_V1, "error-response", "Generic Error Response");

  /**
   * The root of the Echo API v1 response.
   */
  public static final KnownElement RESPONSE_ECHO_V1 =
      new KnownElement(KnownNamespace.RESPONSE_ECHO_V1, "response", "Echo API Response");

  /**
   * The root of the Echo API v2 response.
   */
  public static final KnownElement RESPONSE_ECHO_V2 =
      new KnownElement(KnownNamespace.RESPONSE_ECHO_V2, "response", "Echo API Response");

  /**
   * Support declaration for Anonymous Client Authentication.
   */
  public static final KnownElement SECENTRY_CLIAUTH_NONE_V1 =
      new KnownElement(KnownNamespace.SECENTRY_CLIAUTH_NONE_V1, "anonymous",
          "Support declaration for Anonymous Client Authentication");

  /**
   * Support declaration for TLS Client Certificate Authentication.
   */
  public static final KnownElement SECENTRY_CLIAUTH_TLSCERT_V1 =
      new KnownElement(KnownNamespace.SECENTRY_CLIAUTH_TLSCERT_V1, "tlscert",
          "Support declaration for TLS Client Certificate Authentication");

  /**
   * Support declaration for HTTP Signature Client Authentication.
   */
  public static final KnownElement SECENTRY_CLIAUTH_HTTPSIG_V1 =
      new KnownElement(KnownNamespace.SECENTRY_CLIAUTH_HTTPSIG_V1, "httpsig",
          "Support declaration for HTTP Signature Client Authentication");

  /**
   * Support declaration for TLS Server Certificate Authentication.
   */
  public static final KnownElement SECENTRY_SRVAUTH_TLSCERT_V1 =
      new KnownElement(KnownNamespace.SECENTRY_SRVAUTH_TLSCERT_V1, "tlscert",
          "Support declaration for TLS Server Certificate Authentication");

  /**
   * Support declaration for HTTP Signature Server Authentication.
   */
  public static final KnownElement SECENTRY_SRVAUTH_HTTPSIG_V1 =
      new KnownElement(KnownNamespace.SECENTRY_SRVAUTH_HTTPSIG_V1, "httpsig",
          "Support declaration for HTTP Signature Server Authentication");

  /**
   * Support declaration for "regular TLS" method of request encryption.
   */
  public static final KnownElement SECENTRY_REQENCR_TLS_V1 =
      new KnownElement(KnownNamespace.SECENTRY_REQENCR_TLS_V1, "tls",
          "Support declaration for \"regular TLS\" method of request encryption");

  /**
   * Support declaration for "regular TLS" method of response encryption.
   */
  public static final KnownElement SECENTRY_RESENCR_TLS_V1 =
      new KnownElement(KnownNamespace.SECENTRY_RESENCR_TLS_V1, "tls",
          "Support declaration for \"regular TLS\" method of response encryption");

  private final KnownNamespace namespace;
  private final String elementName;
  private final String humanReadableName;

  private KnownElement(KnownNamespace namespace, String elementName, String humanReadableName) {
    this.namespace = namespace;
    this.elementName = elementName;
    this.humanReadableName = humanReadableName;
  }

  /**
   * @return Plain-text, human-readable name for this element, e.g.
   *         <code>"Discovery Manifest file"</code>, or
   *         <code>"Registry &lt;catalogue&gt; response"</code>.
   */
  public String getHumanReadableName() {
    return this.humanReadableName;
  }

  /**
   * @return The local name of the element.
   */
  public String getLocalName() {
    return this.elementName;
  }

  /**
   * @return The namespace URI of the element.
   */
  public String getNamespaceUri() {
    return this.namespace.getNamespaceUri();
  }

  /**
   * @param element XML element to compare to.
   * @return True, if the given element's root name matches this {@link KnownElement}.
   */
  public boolean matches(Element element) {
    if (element == null) {
      return false;
    }
    if (!element.getNamespaceURI().equals(this.getNamespaceUri())) {
      return false;
    }
    if (!element.getLocalName().equals(this.getLocalName())) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return '{' + this.getNamespaceUri() + '}' + this.getLocalName();
  }
}

