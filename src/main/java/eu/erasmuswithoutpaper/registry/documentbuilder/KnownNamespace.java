package eu.erasmuswithoutpaper.registry.documentbuilder;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.joox.Match;

/**
 * A collection of namespaces commonly used in EWP.
 *
 * <p>
 * The objects include not only namespaceURIs, but also preferred prefixes and default
 * xsi:schemaLocations.
 * </p>
 */
public class KnownNamespace {

  /**
   * As described <a href=
   * 'https://github.com/erasmus-without-paper/ewp-specs-architecture/blob/stable-v1/common-types.xsd'>
   * here</a>.
   */
  public static final KnownNamespace COMMON_TYPES_V1 =
      new KnownNamespace("ewp", "architecture/blob/stable-v1/common-types.xsd",
          "architecture/stable-v1/common-types.xsd", true);

  /**
   * As described <a href=
   * 'https://github.com/erasmus-without-paper/ewp-specs-api-discovery/blob/stable-v4/manifest.xsd'>
   * here</a>.
   */
  public static final KnownNamespace RESPONSE_MANIFEST_V4 = new KnownNamespace("mf",
      "api-discovery/tree/stable-v4", "api-discovery/stable-v4/manifest.xsd", false);

  /**
   * As described <a href=
   * 'https://github.com/erasmus-without-paper/ewp-specs-api-registry/blob/stable-v1/catalogue.xsd'>
   * here</a>.
   */
  public static final KnownNamespace RESPONSE_REGISTRY_V1 = new KnownNamespace("r",
      "api-registry/tree/stable-v1", "api-registry/stable-v1/catalogue.xsd", true);

  /**
   * As described <a href=
   * 'https://github.com/erasmus-without-paper/ewp-specs-api-echo/blob/stable-v1/manifest-entry.xsd'>
   * here</a>.
   */
  public static final KnownNamespace RESPONSE_ECHO_V1 = new KnownNamespace("er1",
      "api-echo/tree/stable-v1", "api-echo/stable-v1/response.xsd", false);

  /**
   * As described <a href=
   * 'https://github.com/erasmus-without-paper/ewp-specs-api-echo/blob/stable-v2/manifest-entry.xsd'>
   * here</a>.
   */
  public static final KnownNamespace RESPONSE_ECHO_V2 = new KnownNamespace("er2",
      "api-echo/tree/stable-v2", "api-echo/stable-v2/response.xsd", false);

  /**
   * As described <a href=
   * 'https://github.com/erasmus-without-paper/ewp-specs-api-discovery/blob/stable-v4/manifest-entry.xsd'>
   * here</a>.
   */
  public static final KnownNamespace APIENTRY_DISCOVERY_V4 =
      new KnownNamespace("d4", "api-discovery/blob/stable-v4/manifest-entry.xsd",
          "api-discovery/stable-v4/manifest-entry.xsd", true);

  /**
   * As described <a href=
   * 'https://github.com/erasmus-without-paper/ewp-specs-api-echo/blob/stable-v1/manifest-entry.xsd'>
   * here</a>.
   */
  public static final KnownNamespace APIENTRY_ECHO_V1 = new KnownNamespace("e1",
      "api-echo/blob/stable-v1/manifest-entry.xsd", "api-echo/stable-v1/manifest-entry.xsd", false);

  /**
   * As described <a href=
   * 'https://github.com/erasmus-without-paper/ewp-specs-api-echo/blob/stable-v2/manifest-entry.xsd'>
   * here</a>.
   */
  public static final KnownNamespace APIENTRY_ECHO_V2 = new KnownNamespace("e2",
      "api-echo/blob/stable-v2/manifest-entry.xsd", "api-echo/stable-v2/manifest-entry.xsd", true);


  /**
   * As described <a href=
   * 'https://github.com/erasmus-without-paper/ewp-specs-api-registry/blob/stable-v1/manifest-entry.xsd'>
   * here</a>.
   */
  public static final KnownNamespace APIENTRY_REGISTRY =
      new KnownNamespace("r1", "api-registry/blob/stable-v1/manifest-entry.xsd",
          "api-registry/stable-v1/manifest-entry.xsd", true);

  /**
   * As described <a href=
   * 'https://github.com/erasmus-without-paper/ewp-specs-api-institutions/blob/stable-v1/manifest-entry.xsd'>
   * here</a>.
   */
  public static final KnownNamespace APIENTRY_INSTITUTIONS_V1 =
      new KnownNamespace("in1", "api-institutions/blob/stable-v1/manifest-entry.xsd",
          "api-institutions/stable-v1/manifest-entry.xsd", false);

  /**
   * As described <a href=
   * 'https://github.com/erasmus-without-paper/ewp-specs-api-institutions/blob/stable-v2/manifest-entry.xsd'>
   * here</a>.
   */
  public static final KnownNamespace APIENTRY_INSTITUTIONS_V2 =
      new KnownNamespace("in2", "api-institutions/blob/stable-v2/manifest-entry.xsd",
          "api-institutions/stable-v2/manifest-entry.xsd", true);

  /**
   * As described <a href=
   * 'https://github.com/erasmus-without-paper/ewp-specs-api-institutions/blob/stable-v1/manifest-entry.xsd'>
   * here</a>.
   */
  public static final KnownNamespace APIENTRY_OUNITS_V1 =
      new KnownNamespace("ou1", "api-ounits/blob/stable-v1/manifest-entry.xsd",
          "api-ounits/stable-v1/manifest-entry.xsd", false);

  /**
   * As described <a href=
   * 'https://github.com/erasmus-without-paper/ewp-specs-api-institutions/blob/stable-v2/manifest-entry.xsd'>
   * here</a>.
   */
  public static final KnownNamespace APIENTRY_OUNITS_V2 =
      new KnownNamespace("ou2", "api-ounits/blob/stable-v2/manifest-entry.xsd",
          "api-ounits/stable-v2/manifest-entry.xsd", true);

  /**
   * As described <a href=
   * 'https://github.com/erasmus-without-paper/ewp-specs-api-iias/blob/stable-v1/manifest-entry.xsd'>
   * here</a>.
   */
  public static final KnownNamespace APIENTRY_IIAS_V1 = new KnownNamespace("ia1",
      "api-iias/blob/stable-v1/manifest-entry.xsd", "api-iias/stable-v1/manifest-entry.xsd", false);

  /**
   * As described <a href=
   * 'https://github.com/erasmus-without-paper/ewp-specs-api-iias/blob/stable-v2/manifest-entry.xsd'>
   * here</a>.
   */
  public static final KnownNamespace APIENTRY_IIAS_V2 = new KnownNamespace("ia2",
      "api-iias/blob/stable-v2/manifest-entry.xsd", "api-iias/stable-v2/manifest-entry.xsd", true);

  /**
   * As described <a href=
   * 'https://github.com/erasmus-without-paper/ewp-specs-api-iia-cnr/blob/stable-v1/manifest-entry.xsd'>
   * here</a>.
   */
  public static final KnownNamespace APIENTRY_IIA_CNR_V1 =
      new KnownNamespace("iac1", "api-iia-cnr/blob/stable-v1/manifest-entry.xsd",
          "api-iia-cnr/stable-v1/manifest-entry.xsd", false);

  /**
   * As described <a href=
   * 'https://github.com/erasmus-without-paper/ewp-specs-api-iia-cnr/blob/stable-v2/manifest-entry.xsd'>
   * here</a>.
   */
  public static final KnownNamespace APIENTRY_IIA_CNR_V2 =
      new KnownNamespace("iac2", "api-iia-cnr/blob/stable-v2/manifest-entry.xsd",
          "api-iia-cnr/stable-v2/manifest-entry.xsd", true);

  private static final String COMMON_URI_PREFIX =
      "https://github.com/erasmus-without-paper/ewp-specs-";
  private static final String COMMON_SCHEMA_LOCATION_PREFIX =
      "https://raw.githubusercontent.com/erasmus-without-paper/ewp-specs-";

  private static final Map<String, KnownNamespace> map_uri2ns;
  private static final Map<String, String> map_prefix2uri;

  static {
    List<KnownNamespace> values = values();
    map_uri2ns = new HashMap<>(values.size());
    map_prefix2uri = new HashMap<>(values.size());
    for (KnownNamespace ns : values) {
      map_uri2ns.put(ns.getNamespaceUri(), ns);
      map_prefix2uri.put(ns.getPreferredPrefix(), ns.getNamespaceUri());
    }
  }

  /**
   * Try to find a {@link KnownNamespace} instance for a given namespaceURI.
   *
   * @param namespaceUri namespaceURI to search for.
   * @return {@link Optional} with the found {@link KnownNamespace} element, or any
   *         {@link Optional#empty()} if not found.
   */
  public static Optional<KnownNamespace> findByNamespaceUri(String namespaceUri) {
    return Optional.ofNullable(map_uri2ns.get(namespaceUri));
  }

  /**
   * A map of all preferred prefixes and their respective namespaces. Useful when using
   * {@link Match#namespaces(Map)}.
   *
   * @return a prefix-&gt;URI map
   */
  public static Map<String, String> prefixMap() {
    return map_prefix2uri;
  }

  /**
   * @return A list of all KnownNamespace constants.
   */
  public static List<KnownNamespace> values() {
    List<KnownNamespace> values = new ArrayList<>();
    for (Field field : KnownNamespace.class.getDeclaredFields()) {
      if (Modifier.isPublic(field.getModifiers()) && Modifier.isStatic(field.getModifiers())) {
        try {
          if (field.get(null) instanceof KnownNamespace) {
            values.add((KnownNamespace) field.get(null));
          }
        } catch (IllegalArgumentException | IllegalAccessException e) {
          throw new RuntimeException(e);
        }
      }
    }
    return values;
  }

  private final String uriEnding;
  private final String preferredPrefix;
  private final String schemaLocEnding;
  private final boolean includeInCatalogueXmlns;

  /**
   * @param preferredPrefix see {@link #getPreferredPrefix()}.
   * @param uriEnding String to be appended to {@link #COMMON_URI_PREFIX} when building namespace
   *        URI.
   * @param schemaLocEnding An extra string to be appended to {@link #COMMON_SCHEMA_LOCATION_PREFIX}
   *        , when building schema location URL.
   */
  private KnownNamespace(String preferredPrefix, String uriEnding, String schemaLocEnding,
      boolean includeInCatalogueXmlns) {
    this.preferredPrefix = preferredPrefix;
    this.uriEnding = uriEnding;
    this.schemaLocEnding = schemaLocEnding;
    this.includeInCatalogueXmlns = includeInCatalogueXmlns;
  }

  /**
   * @return A default XSD location to be used in xsi:schemaLocation attributes.
   */
  public String getDefaultSchemaLocation() {
    return COMMON_SCHEMA_LOCATION_PREFIX + this.schemaLocEnding;
  }

  /**
   * @return The URI of this namespace.
   */
  public String getNamespaceUri() {
    return COMMON_URI_PREFIX + this.uriEnding;
  }

  /**
   * A primary preferred prefix to be used when namespace is being placed in XML documents.
   *
   * @return An XML prefix.
   */
  public String getPreferredPrefix() {
    return this.preferredPrefix;
  }

  /**
   * @return True, if this namespace is supposed to be included in catalogue's root xmlns
   *         declarations.
   */
  public boolean isToBeIncludedInCatalogueXmlns() {
    return this.includeInCatalogueXmlns;
  }
}
