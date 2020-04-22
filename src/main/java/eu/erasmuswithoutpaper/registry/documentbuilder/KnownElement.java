package eu.erasmuswithoutpaper.registry.documentbuilder;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

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
   * API entry for Discovery API v4.
   */
  public static final KnownElement APIENTRY_DISCOVERY_V4 = new KnownElement(
      KnownNamespace.APIENTRY_DISCOVERY_V4, "discovery", "API entry: Discovery Manifest v4");

  /**
   * API entry for Discovery API v5.
   */
  public static final KnownElement APIENTRY_DISCOVERY_V5 = new KnownElement(
      KnownNamespace.APIENTRY_DISCOVERY_V5, "discovery", "API entry: Discovery Manifest v5");

  /**
   * API entry for Registry API v1.
   */
  public static final KnownElement APIENTRY_REGISTRY_V1 =
      new KnownElement(KnownNamespace.APIENTRY_REGISTRY_V1, "registry", "API entry: Registry v1");

  /**
   * API entry for Echo API v1.
   */
  public static final KnownElement APIENTRY_ECHO_V1 =
      new KnownElement(KnownNamespace.APIENTRY_ECHO_V1, "echo", "API entry: Echo v1");

  /**
   * API entry for Echo API v2.
   */
  public static final KnownElement APIENTRY_ECHO_V2 =
      new KnownElement(KnownNamespace.APIENTRY_ECHO_V2, "echo", "API entry: Echo v2");

  /**
   * API entry for Institutions API v1.
   */
  public static final KnownElement APIENTRY_INSTITUTIONS_V1 = new KnownElement(
      KnownNamespace.APIENTRY_INSTITUTIONS_V1, "institutions", "API entry: Institutions v1");

  /**
   * API entry for Institutions API v2.
   */
  public static final KnownElement APIENTRY_INSTITUTIONS_V2 = new KnownElement(
      KnownNamespace.APIENTRY_INSTITUTIONS_V2, "institutions", "API entry: Institutions v2");

  /**
   * API entry for Organizational Units API v1.
   */
  public static final KnownElement APIENTRY_OUNITS_V1 =
      new KnownElement(KnownNamespace.APIENTRY_OUNITS_V1, "organizational-units",
          "API entry: Organizational Units v1");

  /**
   * API entry for Organizational Units API v2.
   */
  public static final KnownElement APIENTRY_OUNITS_V2 =
      new KnownElement(KnownNamespace.APIENTRY_OUNITS_V2, "organizational-units",
          "API entry: Organizational Units v2");

  /**
   * API entry for Courses API v1.
   */
  public static final KnownElement APIENTRY_COURSES_V1 =
      new KnownElement(KnownNamespace.APIENTRY_COURSES_V1, "courses", "API entry: Courses v1");

  /**
   * API entry for Simple Course Replication API v1.
   */
  public static final KnownElement APIENTRY_COURSE_REPLICATION_V1 =
      new KnownElement(KnownNamespace.APIENTRY_COURSE_REPLICATION_V1, "simple-course-replication",
          "API entry: Simple Course Replication v1");

  /**
   * API entry for Interinstitutional Agreements API v1.
   */
  public static final KnownElement APIENTRY_IIAS_V1 = new KnownElement(
      KnownNamespace.APIENTRY_IIAS_V1, "iias", "API entry: Interinstitutional Agreements API v1");

  /**
   * API entry for Interinstitutional Agreements API v2.
   */
  public static final KnownElement APIENTRY_IIAS_V2 = new KnownElement(
      KnownNamespace.APIENTRY_IIAS_V2, "iias", "API entry: Interinstitutional Agreements API v2");

  /**
   * API entry for Interinstitutional Agreement CNR API v1.
   */
  public static final KnownElement APIENTRY_IIA_CNR_V1 =
      new KnownElement(KnownNamespace.APIENTRY_IIA_CNR_V1, "iia-cnr",
          "API entry: Interinstitutional Agreement CNR API v1");

  /**
   * API entry for Interinstitutional Agreement CNR API v2.
   */
  public static final KnownElement APIENTRY_IIA_CNR_V2 =
      new KnownElement(KnownNamespace.APIENTRY_IIA_CNR_V2, "iia-cnr",
          "API entry: Interinstitutional Agreement CNR API v2");

  /**
   * API entry for Outgoing Mobilities API v1.
   */
  public static final KnownElement APIENTRY_OMOBILITIES_V1 =
      new KnownElement(KnownNamespace.APIENTRY_OMOBILITIES_V1, "omobilities",
          "API entry: Outgoing Mobilities API v1");

  /**
   * API entry for Outgoing Mobility LAs API v1.
   */
  public static final KnownElement APIENTRY_OMOBILITY_LAS_V1 =
      new KnownElement(KnownNamespace.APIENTRY_OMOBILITY_LAS_V1, "omobility-las",
          "API entry: Outgoing Mobility LAs API v1");

  /**
   * API entry for Outgoing Mobility CNR API v1.
   */
  public static final KnownElement APIENTRY_OMOBILITY_CNR_V1 =
      new KnownElement(KnownNamespace.APIENTRY_OMOBILITY_CNR_V1, "omobility-cnr",
          "API entry: Outgoing Mobility CNR API v1");

  /**
   * API entry for Outgoing Mobility CNR API v1.
   */
  public static final KnownElement APIENTRY_OMOBILITY_LA_CNR_V1 =
      new KnownElement(KnownNamespace.APIENTRY_OMOBILITY_LA_CNR_V1, "omobility-la-cnr",
          "API entry: Outgoing Mobility LA CNR API v1");

  /**
   * API entry for Incoming Mobilities API v1.
   */
  public static final KnownElement APIENTRY_IMOBILITIES_V1 =
      new KnownElement(KnownNamespace.APIENTRY_IMOBILITIES_V1, "imobilities",
          "API entry: Incoming Mobilities API v1");

  /**
   * API entry for Incoming Mobility CNR API v1.
   */
  public static final KnownElement APIENTRY_IMOBILITY_CNR_V1 =
      new KnownElement(KnownNamespace.APIENTRY_IMOBILITY_CNR_V1, "imobility-cnr",
          "API entry: Incoming Mobility CNR API v1");

  /**
   * API entry for Incoming Mobility ToRs API v1.
   */
  public static final KnownElement APIENTRY_IMOBILITY_TORS_V1 =
      new KnownElement(KnownNamespace.APIENTRY_IMOBILITY_TORS_V1, "imobility-tors",
          "API entry: Incoming Mobility ToRs API v1");

  /**
   * API entry for Incoming Mobility ToR CNR API v1.
   */
  public static final KnownElement APIENTRY_IMOBILITY_TOR_CNR_V1 =
      new KnownElement(KnownNamespace.APIENTRY_IMOBILITY_TOR_CNR_V1, "imobility-tor-cnr",
          "API entry: Incoming Mobility ToR CNR API v1");

  /**
   * The root of the Discovery API v4 response.
   */
  public static final KnownElement RESPONSE_MANIFEST_V4 = new KnownElement(
      KnownNamespace.RESPONSE_MANIFEST_V4, "manifest", "Discovery Manifest v4 file");

  /**
   * The root of the Discovery API v5 response.
   */
  public static final KnownElement RESPONSE_MANIFEST_V5 = new KnownElement(
      KnownNamespace.RESPONSE_MANIFEST_V5, "manifest", "Discovery Manifest v5 file");

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
   * The root of the Institutions API v2 response.
   */
  public static final KnownElement RESPONSE_INSTITUTIONS_V2 =
      new KnownElement(KnownNamespace.RESPONSE_INSTITUTIONS_V2,
          "institutions-response",
          "Institutions API Response"
      );

  /**
   * The root of the Organizational Units API v2 response.
   */
  public static final KnownElement RESPONSE_OUNITS_V2 =
      new KnownElement(KnownNamespace.RESPONSE_OUNITS_V2,
          "ounits-response",
          "Organizational Units API Response"
      );

  /**
   * The root of the Courses API v1 response.
   */
  public static final KnownElement RESPONSE_COURSES_V1 =
      new KnownElement(KnownNamespace.RESPONSE_COURSES_V1,
          "courses-response",
          "Courses v1 API Response"
      );

  /**
   * The root of the Course Replication API v1 response.
   */
  public static final KnownElement RESPONSE_COURSE_REPLICATION_V1 =
      new KnownElement(KnownNamespace.RESPONSE_COURSE_REPLICATION_V1,
          "course-replication-response",
          "Course Replication v1 API Response"
      );

  /**
   * The root of the IIAs API v2 index response.
   */
  public static final KnownElement RESPONSE_IIAS_INDEX_V2 =
      new KnownElement(KnownNamespace.RESPONSE_IIAS_INDEX_V2,
          "iias-index-response",
          "IIAs API v2 Index Response"
      );

  /**
   * The root of the IIAs API v2 get response.
   */
  public static final KnownElement RESPONSE_IIAS_GET_V2 =
      new KnownElement(KnownNamespace.RESPONSE_IIAS_GET_V2,
          "iias-get-response",
          "IIAs API v2 Get Response"
      );

  /**
   * The root of the IIAs API v2 index response.
   */
  public static final KnownElement RESPONSE_IIAS_INDEX_V3 =
      new KnownElement(KnownNamespace.RESPONSE_IIAS_INDEX_V3,
          "iias-index-response",
          "IIAs API v2 Index Response"
      );

  /**
   * The root of the IIAs API v2 get response.
   */
  public static final KnownElement RESPONSE_IIAS_GET_V3 =
      new KnownElement(KnownNamespace.RESPONSE_IIAS_GET_V3,
          "iias-get-response",
          "IIAs API v2 Get Response"
      );

  /**
   * The root of the MT+ Projects API v1 response.
   */
  public static final KnownElement RESPONSE_MT_PROJECTS_V1 =
      new KnownElement(KnownNamespace.RESPONSE_MT_PROJECTS_V1,
          "mt-projects-response",
          "MT+ Projects v1 API Response"
      );

  /**
   * The root of the MT+ Institutions API v1 response.
   */
  public static final KnownElement RESPONSE_MT_INSTITUTIONS_V1 =
      new KnownElement(KnownNamespace.RESPONSE_MT_INSTITUTIONS_V1,
          "mt-institutions-response",
          "MT+ Institutions v1 API Response"
      );

  /**
   * The root of the MT+ Dictionaries API v1 response.
   */
  public static final KnownElement RESPONSE_MT_DICTIONARIES_V1 =
      new KnownElement(KnownNamespace.RESPONSE_MT_DICTIONARIES_V1,
          "mt-dictionaries-response",
          "MT+ Dictionaries v1 API Response"
      );

  /**
   * The root of the Incoming Mobility ToRs API v1 index response.
   */
  public static final KnownElement RESPONSE_IMOBILITY_TORS_INDEX_V1 =
      new KnownElement(KnownNamespace.RESPONSE_IMOBILITY_TORS_INDEX_V1,
          "imobility-tors-index-response",
          "IIAs API v2 Index Response"
      );

  /**
   * The root of the Incoming Mobility ToRs API v1 get response.
   */
  public static final KnownElement RESPONSE_IMOBILITY_TORS_GET_V1 =
      new KnownElement(KnownNamespace.RESPONSE_IMOBILITY_TORS_GET_V1,
          "imobility-tors-get-response",
          "IIAs API v2 Get Response"
      );

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
   * Support declaration for "ewp-rsa-aes128gcm" request encryption.
   */
  public static final KnownElement SECENTRY_REQENCR_EWP_RSA_AES128GCM_V1 =
      new KnownElement(KnownNamespace.SECENTRY_REQENCR_EWP_RSA_AES128GCM_V1, "ewp-rsa-aes128gcm",
          "Support declaration for \"ewp-rsa-aes128gcm\" request encryption method");

  /**
   * Support declaration for "regular TLS" method of response encryption.
   */
  public static final KnownElement SECENTRY_RESENCR_TLS_V1 =
      new KnownElement(KnownNamespace.SECENTRY_RESENCR_TLS_V1, "tls",
          "Support declaration for \"regular TLS\" method of response encryption");

  /**
   * Support declaration for "ewp-rsa-aes128gcm" response encryption.
   */
  public static final KnownElement SECENTRY_RESENCR_EWP_RSA_AES128GCM_V1 =
      new KnownElement(KnownNamespace.SECENTRY_RESENCR_EWP_RSA_AES128GCM_V1, "ewp-rsa-aes128gcm",
          "Support declaration for \"ewp-rsa-aes128gcm\" response encryption method");

  /**
   * @return A list of all {@link KnownElement} constants.
   */
  public static List<KnownElement> values() {
    List<KnownElement> values = new ArrayList<>();
    for (Field field : KnownElement.class.getDeclaredFields()) {
      if (Modifier.isPublic(field.getModifiers()) && Modifier.isStatic(field.getModifiers())) {
        try {
          if (field.get(null) instanceof KnownElement) {
            values.add((KnownElement) field.get(null));
          }
        } catch (IllegalArgumentException | IllegalAccessException e) {
          throw new RuntimeException(e);
        }
      }
    }
    return values;
  }

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
   * @return The namespace preferred prefix of the element.
   */
  public String getNamespacePreferredPrefix() {
    return this.namespace.getPreferredPrefix();
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

