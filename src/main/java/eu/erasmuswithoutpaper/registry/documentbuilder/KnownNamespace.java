package eu.erasmuswithoutpaper.registry.documentbuilder;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import eu.erasmuswithoutpaper.registry.configuration.Constans;

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
   * 'https://github.com/erasmus-without-paper/ewp-specs-api-discovery/blob/stable-v6/manifest.xsd'>
   * here</a>.
   */
  public static final KnownNamespace RESPONSE_MANIFEST_V6 = new KnownNamespace("mf6",
      "api-discovery/tree/stable-v6", "api-discovery/stable-v6/manifest.xsd", false);

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
   * 'https://github.com/erasmus-without-paper/ewp-specs-api-institutions/blob/stable-v2/manifest-entry.xsd'>
   * here</a>.
   */
  public static final KnownNamespace RESPONSE_INSTITUTIONS_V2 = new KnownNamespace("inr2",
      "api-institutions/tree/stable-v2", "api-institutions/stable-v2/response.xsd", false);

  /**
   * As described <a href=
   * 'https://github.com/erasmus-without-paper/ewp-specs-api-courses/blob/master/manifest-entry.xsd'>
   * here</a>.
   */ // TODO: master->stable!
  public static final KnownNamespace RESPONSE_COURSES_V1 = new KnownNamespace("cor1",
      "api-courses/tree/stable-v1", "api-courses/stable-v1/response.xsd", false);
  // TODO: 1. false->true 2. master->stable

  /**
   * As described <a href=
   * 'https://github.com/erasmus-without-paper/ewp-specs-api-course-replication/blob/master/manifest-entry.xsd'>
   * here</a>.
   */ // TODO: master->stable!
  public static final KnownNamespace RESPONSE_COURSE_REPLICATION_V1 = new KnownNamespace("crr1",
      "api-course-replication/tree/stable-v1", "api-course-replication/stable-v1/response.xsd",
      false);
  // TODO: 1. false->true 2. master->stable

  /**
   * As described <a href=
   * 'https://github.com/erasmus-without-paper/ewp-specs-api-ounits/blob/stable-v2/manifest-entry.xsd'>
   * here</a>.
   */
  public static final KnownNamespace RESPONSE_OUNITS_V2 = new KnownNamespace("our2",
      "api-ounits/tree/stable-v2", "api-ounits/stable-v2/response.xsd", false);

  /**
   * As described <a href=
   * 'https://github.com/erasmus-without-paper/ewp-specs-api-iias/blob/stable-v2/manifest-entry.xsd'>
   * here</a>.
   */
  public static final KnownNamespace RESPONSE_IIAS_INDEX_V2 = new KnownNamespace("iari2",
      "api-iias/blob/stable-v2/endpoints/index-response.xsd",
      "api-iias/stable-v2/endpoints/index-response.xsd", false);

  /**
   * As described <a href=
   * 'https://github.com/erasmus-without-paper/ewp-specs-api-iias/blob/stable-v2/manifest-entry.xsd'>
   * here</a>.
   */
  public static final KnownNamespace RESPONSE_IIAS_GET_V2 = new KnownNamespace("iarg2",
      "api-iias/blob/stable-v2/endpoints/get-response.xsd",
      "api-iias/stable-v2/endpoints/get-response.xsd", false);

  /**
   * As described <a href=
   * 'https://github.com/erasmus-without-paper/ewp-specs-api-iias/blob/stable-v3/manifest-entry.xsd'>
   * here</a>.
   */
  public static final KnownNamespace RESPONSE_IIAS_INDEX_V3 = new KnownNamespace("iari3",
      "api-iias/blob/stable-v3/endpoints/index-response.xsd",
      "api-iias/stable-v3/endpoints/index-response.xsd", false);

  /**
   * As described <a href=
   * 'https://github.com/erasmus-without-paper/ewp-specs-api-iias/blob/stable-v3/manifest-entry.xsd'>
   * here</a>.
   */
  public static final KnownNamespace RESPONSE_IIAS_GET_V3 = new KnownNamespace("iarg3",
      "api-iias/blob/stable-v3/endpoints/get-response.xsd",
      "api-iias/stable-v3/endpoints/get-response.xsd", false);

  /**
   * As described <a href=
   * 'https://github.com/erasmus-without-paper/ewp-specs-api-iias/blob/stable-v4/manifest-entry.xsd'>
   * here</a>.
   */
  public static final KnownNamespace RESPONSE_IIAS_INDEX_V4 = new KnownNamespace("iari4",
      "api-iias/blob/stable-v4/endpoints/index-response.xsd",
      "api-iias/stable-v4/endpoints/index-response.xsd", false);

  /**
   * As described <a href=
   * 'https://github.com/erasmus-without-paper/ewp-specs-api-iias/blob/stable-v4/manifest-entry.xsd'>
   * here</a>.
   */
  public static final KnownNamespace RESPONSE_IIAS_GET_V4 = new KnownNamespace("iarg4",
      "api-iias/blob/stable-v4/endpoints/get-response.xsd",
      "api-iias/stable-v4/endpoints/get-response.xsd", false);

  /**
   * As described <a href=
   * 'https://github.com/erasmus-without-paper/ewp-specs-api-iias/blob/stable-v6/manifest-entry.xsd'>
   * here</a>.
   */
  public static final KnownNamespace RESPONSE_IIAS_INDEX_V6 = new KnownNamespace("iari6",
          "api-iias/blob/stable-v6/endpoints/index-response.xsd",
          "api-iias/stable-v6/endpoints/index-response.xsd", false);

  /**
   * As described <a href=
   * 'https://github.com/erasmus-without-paper/ewp-specs-api-iias/blob/stable-v6/manifest-entry.xsd'>
   * here</a>.
   */
  public static final KnownNamespace RESPONSE_IIAS_GET_V6 = new KnownNamespace("iarg6",
          "api-iias/blob/stable-v6/endpoints/get-response.xsd",
          "api-iias/stable-v6/endpoints/get-response.xsd", false);

  /**
   * As described <a href=
   * 'https://github.com/erasmus-without-paper/ewp-specs-api-iias/blob/stable-v7/manifest-entry.xsd'>
   * here</a>.
   */
  public static final KnownNamespace RESPONSE_IIAS_INDEX_V7 = new KnownNamespace("iari7",
          "api-iias/blob/stable-v7/endpoints/index-response.xsd",
          "api-iias/stable-v7/endpoints/index-response.xsd", false);

  /**
   * As described <a href=
   * 'https://github.com/erasmus-without-paper/ewp-specs-api-iias/blob/stable-v7/manifest-entry.xsd'>
   * here</a>.
   */
  public static final KnownNamespace RESPONSE_IIAS_GET_V7 = new KnownNamespace("iarg7",
          "api-iias/blob/stable-v7/endpoints/get-response.xsd",
          "api-iias/stable-v7/endpoints/get-response.xsd", false);

  /**
   * As described <a href=

   * 'https://github.com/erasmus-without-paper/ewp-specs-api-mt-projects/tree/stable-v1/manifest-entry.xsd'>
   * here</a>.
   */
  public static final KnownNamespace RESPONSE_MT_PROJECTS_V1 = new KnownNamespace("mtpr1",
      "api-mt-projects/tree/stable-v1",
      "api-mt-projects/stable-v1/response.xsd",
      false);

  /**
   * As described <a href=
   * 'https://github.com/erasmus-without-paper/ewp-specs-api-mt-institutions/tree/stable-v1/manifest-entry.xsd'>
   * here</a>.
   */
  public static final KnownNamespace RESPONSE_MT_INSTITUTIONS_V1 = new KnownNamespace("mtir1",
      "api-mt-institutions/tree/stable-v1",
      "api-mt-institutions/stable-v1/response.xsd",
      false);

  /**
   * As described <a href=
   * 'https://github.com/erasmus-without-paper/ewp-specs-api-mt-dictionaries/tree/stable-v1/manifest-entry.xsd'>
   * here</a>.
   */
  public static final KnownNamespace RESPONSE_MT_DICTIONARIES_V1 = new KnownNamespace("mtdr1",
      "api-mt-dictionaries/tree/stable-v1",
      "api-mt-dictionaries/stable-v1/response.xsd",
      false);

  /**
   * As described <a href=
   * 'https://github.com/erasmus-without-paper/ewp-specs-api-imobilities/tree/stable-v1/manifest-entry.xsd'>
   * here</a>.
   */
  public static final KnownNamespace RESPONSE_IMOBILITIES_GET_V1 = new KnownNamespace("img1",
      "api-imobilities/blob/stable-v1/endpoints/get-response.xsd",
      "api-imobilities/stable-v1/endpoints/get-response.xsd", false);

  /**
   * As described <a href=
   * 'https://github.com/erasmus-without-paper/ewp-specs-api-imobilities/tree/stable-v2/manifest-entry.xsd'>
   * here</a>.
   */
  public static final KnownNamespace RESPONSE_IMOBILITIES_GET_V2 = new KnownNamespace("img2",
      "api-imobilities/blob/stable-v2/endpoints/get-response.xsd",
      "api-imobilities/stable-v2/endpoints/get-response.xsd", false);

  /**
   * As described <a href=
   * 'https://github.com/erasmus-without-paper/ewp-specs-api-imobility-tors/tree/stable-v1/manifest-entry.xsd'>
   * here</a>.
   */
  public static final KnownNamespace RESPONSE_IMOBILITY_TORS_INDEX_V1 = new KnownNamespace("imtri1",
      "api-imobility-tors/blob/stable-v1/endpoints/index-response.xsd",
      "api-imobility-tors/stable-v1/endpoints/index-response.xsd", false);
  // TODO false -> true

  /**
   * As described <a href=
   * 'https://github.com/erasmus-without-paper/ewp-specs-api-imobility-tors/tree/stable-v2/manifest-entry.xsd'>
   * here</a>.
   */
  public static final KnownNamespace RESPONSE_IMOBILITY_TORS_INDEX_V2 = new KnownNamespace("imtri2",
      "api-imobility-tors/blob/stable-v2/endpoints/index-response.xsd",
      "api-imobility-tors/stable-v2/endpoints/index-response.xsd", false);
  // TODO false -> true

  /**
   * As described <a href=
   * 'https://github.com/erasmus-without-paper/ewp-specs-api-imobility-tors/tree/stable-v1/manifest-entry.xsd'>
   * here</a>.
   */
  public static final KnownNamespace RESPONSE_IMOBILITY_TORS_GET_V1 = new KnownNamespace("imtrg1",
      "api-imobility-tors/blob/stable-v1/endpoints/get-response.xsd",
      "api-imobility-tors/stable-v1/endpoints/get-response.xsd", false);
  // TODO false -> true

  /**
   * As described <a href=
   * 'https://github.com/erasmus-without-paper/ewp-specs-api-imobility-tors/tree/stable-v2/manifest-entry.xsd'>
   * here</a>.
   */
  public static final KnownNamespace RESPONSE_IMOBILITY_TORS_GET_V2 = new KnownNamespace("imtrg2",
      "api-imobility-tors/blob/stable-v2/endpoints/get-response.xsd",
      "api-imobility-tors/stable-v2/endpoints/get-response.xsd", false);
  // TODO false -> true

  /**
   * As described <a href=
   * 'https://github.com/erasmus-without-paper/ewp-specs-api-factsheet/blob/master/manifest-entry.xsd'>
   * here</a>.
   */ // TODO: master->stable!
  public static final KnownNamespace RESPONSE_FACTSHEET_V1 = new KnownNamespace("fr1",
      "api-factsheet/tree/stable-v1", "api-factsheet/stable-v1/response.xsd", false);
  // TODO: 1. false->true 2. master->stable

  /**
   * As described <a href=
   * 'https://github.com/erasmus-without-paper/ewp-specs-api-omobilities/tree/stable-v1/manifest-entry.xsd'>
   * here</a>.
   */
  public static final KnownNamespace RESPONSE_OMOBILITIES_INDEX_V1 = new KnownNamespace("omri1",
      "api-omobilities/blob/stable-v1/endpoints/index-response.xsd",
      "api-omobilities/stable-v1/endpoints/index-response.xsd", false);
  // TODO false -> true

  /**
   * As described <a href=
   * 'https://github.com/erasmus-without-paper/ewp-specs-api-omobilities/tree/stable-v1/manifest-entry.xsd'>
   * here</a>.
   */
  public static final KnownNamespace RESPONSE_OMOBILITIES_GET_V1  = new KnownNamespace("omrg1",
      "api-omobilities/blob/stable-v1/endpoints/get-response.xsd",
      "api-omobilities/stable-v1/endpoints/get-response.xsd", false);
  // TODO false -> true

  /**
   * As described <a href=
   * 'https://github.com/erasmus-without-paper/ewp-specs-api-omobilities/tree/stable-v2/manifest-entry.xsd'>
   * here</a>.
   */
  public static final KnownNamespace RESPONSE_OMOBILITIES_INDEX_V2 = new KnownNamespace("omri2",
      "api-omobilities/blob/stable-v2/endpoints/index-response.xsd",
      "api-omobilities/stable-v2/endpoints/index-response.xsd", false);
  // TODO false -> true

  /**
   * As described <a href=
   * 'https://github.com/erasmus-without-paper/ewp-specs-api-omobilities/tree/stable-v3/manifest-entry.xsd'>
   * here</a>.
   */
  public static final KnownNamespace RESPONSE_OMOBILITIES_INDEX_V3 =
      new KnownNamespace("omri3", "api-omobilities/blob/stable-v3/endpoints/index-response.xsd",
          "api-omobilities/stable-v3/endpoints/index-response.xsd", false);

  /**
   * As described <a href=
   * 'https://github.com/erasmus-without-paper/ewp-specs-api-omobilities/tree/stable-v2/manifest-entry.xsd'>
   * here</a>.
   */
  public static final KnownNamespace RESPONSE_OMOBILITIES_GET_V2  = new KnownNamespace("omrg2",
      "api-omobilities/blob/stable-v2/endpoints/get-response.xsd",
      "api-omobilities/stable-v2/endpoints/get-response.xsd", false);
  // TODO false -> true

  /**
   * As described <a href=
   * 'https://github.com/erasmus-without-paper/ewp-specs-api-omobilities/tree/stable-v3/manifest-entry.xsd'>
   * here</a>.
   */
  public static final KnownNamespace RESPONSE_OMOBILITIES_GET_V3  = new KnownNamespace("omrg3",
          "api-omobilities/blob/stable-v3/endpoints/get-response.xsd",
          "api-omobilities/stable-v3/endpoints/get-response.xsd", false);

  /**
   * As described <a href=
   * 'https://github.com/erasmus-without-paper/ewp-specs-api-omobilities/tree/stable-v1/manifest-entry.xsd'>
   * here</a>.
   */
  public static final KnownNamespace RESPONSE_OMOBILITIES_STATS_V1 = new KnownNamespace("omsr1",
      "api-omobility-stats/tree/stable-v1",
      "api-omobility-stats/stable-v1/endpoints/response.xsd", false);

  /**
   * As described <a href=
   * 'https://github.com/erasmus-without-paper/ewp-specs-api-omobility-las/blob/master/manifest-entry.xsd'>
   * here</a>.
   */ // TODO: master->stable!
  public static final KnownNamespace RESPONSE_OMOBILITY_LAS_GET_V1  = new KnownNamespace("omlrg1",
      "api-omobility-las/blob/stable-v1/endpoints/get-response.xsd",
      "api-omobility-las/blob/master/get-response.xsd", false);
  // TODO: 1. false->true 2. master->stable

  /**
   * As described <a href=
   * 'https://github.com/erasmus-without-paper/ewp-specs-api-omobility-las/blob/master/manifest-entry.xsd'>
   * here</a>.
   */ // TODO: master->stable!
  public static final KnownNamespace RESPONSE_OMOBILITY_LAS_INDEX_V1  = new KnownNamespace("omlri1",
      "api-omobility-las/blob/stable-v1/endpoints/index-response.xsd",
      "api-omobility-las/blob/master/index-response.xsd", false);
  // TODO: 1. false->true 2. master->stable

  /**
   * As described <a href=
   * 'https://github.com/erasmus-without-paper/ewp-specs-api-omobility-las/blob/master/manifest-entry.xsd'>
   * here</a>.
   */ // TODO: master->stable!
  public static final KnownNamespace RESPONSE_OMOBILITY_LAS_UPDATE_V1  = new KnownNamespace(
      "omlru1",
      "api-omobility-las/blob/stable-v1/endpoints/update-response.xsd",
      "api-omobility-las/blob/master/update-response.xsd", false);
  // TODO: 1. false->true 2. master->stable


  /**
   * As described <a href=
   * 'https://github.com/erasmus-without-paper/ewp-specs-api-discovery/blob/stable-v6/manifest-entry.xsd'>
   * here</a>.
   */
  public static final KnownNamespace APIENTRY_DISCOVERY_V6 =
      new KnownNamespace("d6", "api-discovery/blob/stable-v6/manifest-entry.xsd",
          "api-discovery/stable-v6/manifest-entry.xsd", true);

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
  public static final KnownNamespace APIENTRY_REGISTRY_V1 =
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
   * 'https://github.com/erasmus-without-paper/ewp-specs-api-ounits/blob/stable-v1/manifest-entry.xsd'>
   * here</a>.
   */
  public static final KnownNamespace APIENTRY_OUNITS_V1 =
      new KnownNamespace("ou1", "api-ounits/blob/stable-v1/manifest-entry.xsd",
          "api-ounits/stable-v1/manifest-entry.xsd", false);

  /**
   * As described <a href=
   * 'https://github.com/erasmus-without-paper/ewp-specs-api-ounits/blob/stable-v2/manifest-entry.xsd'>
   * here</a>.
   */
  public static final KnownNamespace APIENTRY_OUNITS_V2 =
      new KnownNamespace("ou2", "api-ounits/blob/stable-v2/manifest-entry.xsd",
          "api-ounits/stable-v2/manifest-entry.xsd", true);

  /**
   * As described <a href=
   * 'https://github.com/erasmus-without-paper/ewp-specs-api-courses/blob/master/manifest-entry.xsd'>
   * here</a>.
   */ // TODO: master->stable!
  public static final KnownNamespace APIENTRY_COURSES_V1 =
      new KnownNamespace("co1", "api-courses/blob/stable-v1/manifest-entry.xsd",
          "api-courses/master/manifest-entry.xsd", false);
  // TODO: 1. false->true 2. master->stable

  /**
   * As described <a href=
   * 'https://github.com/erasmus-without-paper/ewp-specs-api-course-replication/blob/master/manifest-entry.xsd'>
   * here</a>.
   */ // TODO: master->stable!
  public static final KnownNamespace APIENTRY_COURSE_REPLICATION_V1 =
      new KnownNamespace("cr1", "api-course-replication/blob/stable-v1/manifest-entry.xsd",
          "api-course-replication/master/manifest-entry.xsd", false);
  // TODO: 1. false->true 2. master->stable

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
   * 'https://github.com/erasmus-without-paper/ewp-specs-api-iias/blob/stable-v3/manifest-entry.xsd'>
   * here</a>.
   */
  public static final KnownNamespace APIENTRY_IIAS_V3 = new KnownNamespace("ia3",
      "api-iias/blob/stable-v3/manifest-entry.xsd", "api-iias/stable-v3/manifest-entry.xsd", false);

  /**
   * As described <a href=
   * 'https://github.com/erasmus-without-paper/ewp-specs-api-iias/blob/stable-v4/manifest-entry.xsd'>
   * here</a>.
   */
  public static final KnownNamespace APIENTRY_IIAS_V4 = new KnownNamespace("ia4",
      "api-iias/blob/stable-v4/manifest-entry.xsd", "api-iias/stable-v4/manifest-entry.xsd", false);

  /**
   * As described <a href=
   * 'https://github.com/erasmus-without-paper/ewp-specs-api-iias/blob/stable-v5/manifest-entry.xsd'>
   * here</a>.
   */
  public static final KnownNamespace APIENTRY_IIAS_V5 = new KnownNamespace("ia5",
      "api-iias/blob/stable-v5/manifest-entry.xsd", "api-iias/stable-v5/manifest-entry.xsd", false);

  /**
   * As described <a href=
   * 'https://github.com/erasmus-without-paper/ewp-specs-api-iias/blob/stable-v6/manifest-entry.xsd'>
   * here</a>.
   */
  public static final KnownNamespace APIENTRY_IIAS_V6 = new KnownNamespace("ia6",
      "api-iias/blob/stable-v6/manifest-entry.xsd", "api-iias/stable-v6/manifest-entry.xsd", false);

  /**
   * As described <a href=
   * 'https://github.com/erasmus-without-paper/ewp-specs-api-iias/blob/stable-v6/manifest-entry.xsd'>
   * here</a>.
   */
  public static final KnownNamespace APIENTRY_IIAS_V7 = new KnownNamespace("ia7",
      "api-iias/blob/stable-v7/manifest-entry.xsd", "api-iias/stable-v7/manifest-entry.xsd", false);

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

  /**
   * As described <a href=
   * 'https://github.com/erasmus-without-paper/ewp-specs-api-iia-cnr/blob/stable-v3/manifest-entry.xsd'>
   * here</a>.
   */
  public static final KnownNamespace APIENTRY_IIA_CNR_V3 =
      new KnownNamespace("iac3", "api-iia-cnr/blob/stable-v3/manifest-entry.xsd",
          "api-iia-cnr/stable-v3/manifest-entry.xsd", true);

  /**
   * As described <a href=
   * 'https://github.com/erasmus-without-paper/ewp-specs-api-iias-approval/blob/master/manifest-entry.xsd'>
   * here</a>.
   */ // TODO: master->stable!
  public static final KnownNamespace APIENTRY_IIAS_APPROVAL_V1 =
      new KnownNamespace("iaa1", "api-iias-approval/blob/stable-v1/manifest-entry.xsd",
          "api-iias-approval/master/manifest-entry.xsd", false);
  // TODO: 1. false->true 2. master->stable

  /**
   * As described <a href=
   * 'https://github.com/erasmus-without-paper/ewp-specs-api-iias-approval/blob/stable-v2/manifest-entry.xsd'>
   * here</a>.
   */
  public static final KnownNamespace APIENTRY_IIAS_APPROVAL_V2 =
      new KnownNamespace("iaa2", "api-iias-approval/blob/stable-v2/manifest-entry.xsd",
          "api-iias-approval/stable-v2/manifest-entry.xsd", false);
  // TODO: 2. false->true

  /**
   * As described <a href=
   * 'https://github.com/erasmus-without-paper/ewp-specs-api-iia-approval-cnr/blob/master/manifest-entry.xsd'>
   * here</a>.
   */ // TODO: master->stable!
  public static final KnownNamespace APIENTRY_IIA_APPROVAL_CNR_V1 =
      new KnownNamespace("iaac1", "api-iia-approval-cnr/blob/stable-v1/manifest-entry.xsd",
          "api-iia-approval-cnr/master/manifest-entry.xsd", false);
  // TODO: 1. false->true 2. master->stable

  /**
   * As described <a href=
   * 'https://github.com/erasmus-without-paper/ewp-specs-api-iia-approval-cnr/blob/stable-v2/manifest-entry.xsd'>
   * here</a>.
   */
  public static final KnownNamespace APIENTRY_IIA_APPROVAL_CNR_V2 =
      new KnownNamespace("iaac2", "api-iia-approval-cnr/blob/stable-v2/manifest-entry.xsd",
          "api-iia-approval-cnr/stable-v2/manifest-entry.xsd", false);
  // TODO: 2. false->true

  /**
   * As described <a href=
   * 'https://github.com/erasmus-without-paper/ewp-specs-api-omobilities/blob/stable-v1/manifest-entry.xsd'>
   * here</a>.
   */
  public static final KnownNamespace APIENTRY_OMOBILITIES_V1 =
      new KnownNamespace("om1", "api-omobilities/blob/stable-v1/manifest-entry.xsd",
          "api-omobilities/stable-v1/manifest-entry.xsd", false);
  // TODO: false->true

  /**
   * As described <a href=
   * 'https://github.com/erasmus-without-paper/ewp-specs-api-omobilities/blob/stable-v2/manifest-entry.xsd'>
   * here</a>.
   */
  public static final KnownNamespace APIENTRY_OMOBILITIES_V2 =
      new KnownNamespace("om2", "api-omobilities/blob/stable-v2/manifest-entry.xsd",
          "api-omobilities/stable-v2/manifest-entry.xsd", false);

  /**
   * As described <a href=
   * 'https://github.com/erasmus-without-paper/ewp-specs-api-omobilities/blob/stable-v3/manifest-entry.xsd'>
   * here</a>.
   */
  public static final KnownNamespace APIENTRY_OMOBILITIES_V3 =
      new KnownNamespace("om3", "api-omobilities/blob/stable-v3/manifest-entry.xsd",
          "api-omobilities/stable-v3/manifest-entry.xsd", false);

  /**
   * As described <a href=
   * 'https://github.com/erasmus-without-paper/ewp-specs-api-omobility-las/blob/master/manifest-entry.xsd'>
   * here</a>.
   */ // TODO: master->stable!
  public static final KnownNamespace APIENTRY_OMOBILITY_LAS_V1 =
      new KnownNamespace("oml1", "api-omobility-las/blob/stable-v1/manifest-entry.xsd",
          "api-omobility-las/master/manifest-entry.xsd", false);
  // TODO: 1. false->true 2. master->stable

  /**
   * As described <a href=
   * 'https://github.com/erasmus-without-paper/ewp-specs-api-omobility-cnr/blob/stable-v1/manifest-entry.xsd'>
   * here</a>.
   */
  public static final KnownNamespace APIENTRY_OMOBILITY_CNR_V1 =
      new KnownNamespace("omc1", "api-omobility-cnr/blob/stable-v1/manifest-entry.xsd",
          "api-omobility-cnr/master/manifest-entry.xsd", false);

  /**
   * As described <a href=
   * 'https://github.com/erasmus-without-paper/ewp-specs-api-omobility-cnr/blob/stable-v2/manifest-entry.xsd'>
   * here</a>.
   */
  public static final KnownNamespace APIENTRY_OMOBILITY_CNR_V2 =
      new KnownNamespace("omc2", "api-omobility-cnr/blob/stable-v2/manifest-entry.xsd",
          "api-omobility-cnr/master/manifest-entry.xsd", false);

  /**
   * As described <a href=
   * 'https://github.com/erasmus-without-paper/ewp-specs-api-omobility-stats/blob/stable-v1/manifest-entry.xsd'>
   * here</a>.
   */
  public static final KnownNamespace APIENTRY_OMOBILITY_STATS_V1 =
      new KnownNamespace("oms1", "api-omobility-stats/blob/stable-v1/manifest-entry.xsd",
          "api-omobility-stats/master/manifest-entry.xsd", false);

  /**
   * As described <a href=
   * 'https://github.com/erasmus-without-paper/ewp-specs-api-omobility-la-cnr/blob/master/manifest-entry.xsd'>
   * here</a>.
   */ // TODO: master->stable!
  public static final KnownNamespace APIENTRY_OMOBILITY_LA_CNR_V1 =
      new KnownNamespace("omlc1", "api-omobility-la-cnr/blob/stable-v1/manifest-entry.xsd",
          "api-omobility-la-cnr/master/manifest-entry.xsd", false);
  // TODO: 1. false->true 2. master->stable

  /**
   * As described <a href=
   * 'https://github.com/erasmus-without-paper/ewp-specs-api-imobilities/blob/stable-v1/manifest-entry.xsd'>
   * here</a>.
   */
  public static final KnownNamespace APIENTRY_IMOBILITIES_V1 =
      new KnownNamespace("im1", "api-imobilities/blob/stable-v1/manifest-entry.xsd",
          "api-imobilities/stable-v1/manifest-entry.xsd", false);

  /**
   * As described <a href=
   * 'https://github.com/erasmus-without-paper/ewp-specs-api-imobilities/blob/stable-v2/manifest-entry.xsd'>
   * here</a>.
   */
  public static final KnownNamespace APIENTRY_IMOBILITIES_V2 =
      new KnownNamespace("im2", "api-imobilities/blob/stable-v2/manifest-entry.xsd",
          "api-imobilities/stable-v2/manifest-entry.xsd", false);

  /**
   * As described <a href=
   * 'https://github.com/erasmus-without-paper/ewp-specs-api-imobility-cnr/blob/stable-v1/manifest-entry.xsd'>
   * here</a>.
   */
  public static final KnownNamespace APIENTRY_IMOBILITY_CNR_V1 =
      new KnownNamespace("imc1", "api-imobility-cnr/blob/stable-v1/manifest-entry.xsd",
          "api-imobility-cnr/stable-v1/manifest-entry.xsd", false);

  /**
   * As described <a href=
   * 'https://github.com/erasmus-without-paper/ewp-specs-api-imobility-cnr/blob/stable-v2/manifest-entry.xsd'>
   * here</a>.
   */
  public static final KnownNamespace APIENTRY_IMOBILITY_CNR_V2 =
      new KnownNamespace("imc2", "api-imobility-cnr/blob/stable-v2/manifest-entry.xsd",
          "api-imobility-cnr/stable-v2/manifest-entry.xsd", false);

  /**
   * As described <a href=
   * 'https://github.com/erasmus-without-paper/ewp-specs-api-imobility-tors/blob/master/manifest-entry.xsd'>
   * here</a>.
   */ // TODO: master->stable!
  public static final KnownNamespace APIENTRY_IMOBILITY_TORS_V1 =
      new KnownNamespace("imt1", "api-imobility-tors/blob/stable-v1/manifest-entry.xsd",
          "api-imobility-tors/master/manifest-entry.xsd", false);
  // TODO: 1. false->true

  /**
   * As described <a href=
   * 'https://github.com/erasmus-without-paper/ewp-specs-api-imobility-tors/blob/stable-v2/manifest-entry.xsd'>
   * here</a>.
   */
  public static final KnownNamespace APIENTRY_IMOBILITY_TORS_V2 =
      new KnownNamespace("imt2", "api-imobility-tors/blob/stable-v2/manifest-entry.xsd",
          "api-imobility-tors/master/manifest-entry.xsd", false);

  /**
   * As described <a href=
   * 'https://github.com/erasmus-without-paper/ewp-specs-api-imobility-tor-cnr/blob/master/manifest-entry.xsd'>
   * here</a>.
   */ // TODO: master->stable!
  public static final KnownNamespace APIENTRY_IMOBILITY_TOR_CNR_V1 =
      new KnownNamespace("imtc1", "api-imobility-tor-cnr/blob/stable-v1/manifest-entry.xsd",
          "api-imobility-tor-cnr/master/manifest-entry.xsd", false);
  // TODO: 1. false->true 2. master->stable

  /**
   * As described <a href=
   * 'https://github.com/erasmus-without-paper/ewp-specs-api-mt-projects/blob/stable-v1/manifest-entry.xsd'>
   * here</a>.
   */
  public static final KnownNamespace APIENTRY_MT_PROJECTS_V1 =
      new KnownNamespace("mtp1", "api-mt-projects/blob/stable-v1/manifest-entry.xsd",
          "api-mt-projects/stable-v1/manifest-entry.xsd", false);
  // TODO: 1. false->true

  /**
   * As described <a href=
   * 'https://github.com/erasmus-without-paper/ewp-specs-api-mt-institutions/blob/stable-v1/manifest-entry.xsd'>
   * here</a>.
   */
  public static final KnownNamespace APIENTRY_MT_INSTITUTIONS_V1 =
      new KnownNamespace("mti1", "api-mt-institutions/blob/stable-v1/manifest-entry.xsd",
          "api-mt-institutions/stable-v1/manifest-entry.xsd", false);
  // TODO: 1. false->true

  /**
   * As described <a href=
   * 'https://github.com/erasmus-without-paper/ewp-specs-api-mt-dictionaries/blob/stable-v1/manifest-entry.xsd'>
   * here</a>.
   */
  public static final KnownNamespace APIENTRY_MT_DICTIONARIES_V1 =
      new KnownNamespace("mtd1", "api-mt-dictionaries/blob/stable-v1/manifest-entry.xsd",
          "api-mt-dictionaries/stable-v1/manifest-entry.xsd", false);
  // TODO: 1. false->true
  /**
   * As described <a href=
   * 'https://github.com/erasmus-without-paper/ewp-specs-api-factsheet/blob/master/manifest-entry.xsd'>
   * here</a>.
   */ // TODO: master->stable!
  public static final KnownNamespace APIENTRY_FACTSHEET_V1 =
      new KnownNamespace("f1", "api-factsheet/blob/stable-v1/manifest-entry.xsd",
          "api-factsheet/master/manifest-entry.xsd", false);
  // TODO: 1. false->true

  /**
   * As described <a href=
   * 'https://github.com/erasmus-without-paper/ewp-specs-api-factsheet/blob/stable-v1/manifest-entry.xsd'>
   * here</a>.
   */
  public static final KnownNamespace APIENTRY_FILE_V1 =
      new KnownNamespace("file1", "api-file/blob/stable-v1/manifest-entry.xsd",
          "api-file/master/manifest-entry.xsd", false);

  /**
   * As described <a href=
   * 'https://github.com/erasmus-without-paper/ewp-specs-sec-intro/blob/stable-v2/schema.xsd'>here
   * </a>.
   */
  public static final KnownNamespace SEC_V2_COMMON =
      new KnownNamespace("sec", "sec-intro/tree/stable-v2", "sec-intro/stable-v2/schema.xsd", true);

  /**
   * As described <a href=
   * 'https://github.com/erasmus-without-paper/ewp-specs-sec-cliauth-none/blob/stable-v1/security-entries.xsd'>
   * here</a>.
   */
  public static final KnownNamespace SECENTRY_CLIAUTH_NONE_V1 = new KnownNamespace("sec-A0",
      "sec-cliauth-none/tree/stable-v1", "sec-cliauth-none/stable-v1/security-entries.xsd", true);

  /**
   * As described <a href=
   * 'https://github.com/erasmus-without-paper/ewp-specs-sec-cliauth-tlscert/blob/stable-v1/security-entries.xsd'>
   * here</a>.
   */
  public static final KnownNamespace SECENTRY_CLIAUTH_TLSCERT_V1 =
      new KnownNamespace("sec-A1", "sec-cliauth-tlscert/tree/stable-v1",
          "sec-cliauth-tlscert/stable-v1/security-entries.xsd", true);

  /**
   * As described <a href=
   * 'https://github.com/erasmus-without-paper/ewp-specs-sec-cliauth-httpsig/blob/stable-v1/security-entries.xsd'>
   * here</a>.
   */
  public static final KnownNamespace SECENTRY_CLIAUTH_HTTPSIG_V1 =
      new KnownNamespace("sec-A2", "sec-cliauth-httpsig/tree/stable-v1",
          "sec-cliauth-httpsig/stable-v1/security-entries.xsd", true);

  /**
   * As described <a href=
   * 'https://github.com/erasmus-without-paper/ewp-specs-sec-srvauth-tlscert/blob/stable-v1/security-entries.xsd'>
   * here</a>.
   */
  public static final KnownNamespace SECENTRY_SRVAUTH_TLSCERT_V1 =
      new KnownNamespace("sec-B1", "sec-srvauth-tlscert/tree/stable-v1",
          "sec-srvauth-tlscert/stable-v1/security-entries.xsd", true);

  /**
   * As described <a href=
   * 'https://github.com/erasmus-without-paper/ewp-specs-sec-srvauth-httpsig/blob/stable-v1/security-entries.xsd'>
   * here</a>.
   */
  public static final KnownNamespace SECENTRY_SRVAUTH_HTTPSIG_V1 =
      new KnownNamespace("sec-B2", "sec-srvauth-httpsig/tree/stable-v1",
          "sec-srvauth-httpsig/stable-v1/security-entries.xsd", true);

  /**
   * As described <a href=
   * 'https://github.com/erasmus-without-paper/ewp-specs-sec-reqencr-tls/blob/stable-v1/security-entries.xsd'>
   * here</a>.
   */
  public static final KnownNamespace SECENTRY_REQENCR_TLS_V1 = new KnownNamespace("sec-C1",
      "sec-reqencr-tls/tree/stable-v1", "sec-reqencr-tls/stable-v1/security-entries.xsd", true);

  /**
   * As described <a href=
   * 'https://github.com/erasmus-without-paper/ewp-specs-sec-reqencr-tls/blob/stable-v1/security-entries.xsd'>
   * here</a>.
   */
  public static final KnownNamespace SECENTRY_REQENCR_EWP_RSA_AES128GCM_V1 =
      new KnownNamespace("sec-C2", "sec-reqencr-rsa-aes128gcm/tree/stable-v1",
          "sec-reqencr-rsa-aes128gcm/stable-v1/security-entries.xsd", false);
  // TODO: false->true

  /**
   * As described <a href=
   * 'https://github.com/erasmus-without-paper/ewp-specs-sec-resencr-tls/blob/stable-v1/security-entries.xsd'>
   * here</a>.
   */
  public static final KnownNamespace SECENTRY_RESENCR_TLS_V1 = new KnownNamespace("sec-D1",
      "sec-resencr-tls/tree/stable-v1", "sec-resencr-tls/stable-v1/security-entries.xsd", true);

  /**
   * As described <a href=
   * 'https://github.com/erasmus-without-paper/ewp-specs-sec-resencr-tls/blob/stable-v1/security-entries.xsd'>
   * here</a>.
   */
  public static final KnownNamespace SECENTRY_RESENCR_EWP_RSA_AES128GCM_V1 =
      new KnownNamespace("sec-D2", "sec-resencr-rsa-aes128gcm/tree/stable-v1",
          "sec-resencr-rsa-aes128gcm/stable-v1/security-entries.xsd", false);
  // TODO: false->true

  private static final String COMMON_URI_PREFIX =
      Constans.REGISTRY_REPO_URL + "/ewp-specs-";
  private static final String COMMON_SCHEMA_LOCATION_PREFIX =
      Constans.SCHEMA_BASE_URL + "/ewp-specs-";

  private static final Map<String, KnownNamespace> MAP_URI2NS;
  private static final Map<String, String> MAP_PREFIX2URI;

  static {
    List<KnownNamespace> values = values();
    MAP_URI2NS = new HashMap<>(values.size());
    MAP_PREFIX2URI = new HashMap<>(values.size());
    for (KnownNamespace ns : values) {
      MAP_URI2NS.put(ns.getNamespaceUri(), ns);
      if (MAP_PREFIX2URI.containsKey(ns.getPreferredPrefix())) {
        throw new RuntimeException("Namespace prefix conflict: " + ns.getPreferredPrefix());
      }
      MAP_PREFIX2URI.put(ns.getPreferredPrefix(), ns.getNamespaceUri());
    }
  }

  /**
   * Try to find a {@link KnownNamespace} instance for a given namespaceURI.
   *
   * @param namespaceUri
   *     namespaceURI to search for.
   * @return {@link Optional} with the found {@link KnownNamespace} element, or any
   *     {@link Optional#empty()} if not found.
   */
  public static Optional<KnownNamespace> findByNamespaceUri(String namespaceUri) {
    return Optional.ofNullable(MAP_URI2NS.get(namespaceUri));
  }

  /**
   * A map of all preferred prefixes and their respective namespaces. Useful when using
   * {@link Match#namespaces(Map)}.
   *
   * @return a prefix-&gt;URI map
   */
  public static Map<String, String> prefixMap() {
    return MAP_PREFIX2URI;
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
   * @param preferredPrefix
   *     see {@link #getPreferredPrefix()}.
   * @param uriEnding
   *     String to be appended to {@link #COMMON_URI_PREFIX} when building namespace
   *     URI.
   * @param schemaLocEnding
   *     An extra string to be appended to {@link #COMMON_SCHEMA_LOCATION_PREFIX}
   *     , when building schema location URL.
   */
  public KnownNamespace(String preferredPrefix, String uriEnding, String schemaLocEnding,
      boolean includeInCatalogueXmlns) {
    this.preferredPrefix = preferredPrefix;
    this.uriEnding = uriEnding;
    if (this.uriEnding.startsWith("ewp-specs-")) {
      throw new RuntimeException("Drop the 'ewp-specs-' prefix!");
    }
    this.schemaLocEnding = schemaLocEnding;
    if (this.schemaLocEnding.startsWith("ewp-specs-")) {
      throw new RuntimeException("Drop the 'ewp-specs-' prefix!");
    }
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
   *     declarations.
   */
  public boolean isToBeIncludedInCatalogueXmlns() {
    return this.includeInCatalogueXmlns;
  }
}
