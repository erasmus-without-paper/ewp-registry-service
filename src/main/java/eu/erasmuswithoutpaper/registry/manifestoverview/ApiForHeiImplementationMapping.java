package eu.erasmuswithoutpaper.registry.manifestoverview;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Defines a mapping between API implemented for HEI to place (manifest, host, version) where it
 * is implemented.
 */
public class ApiForHeiImplementationMapping {
  private Map<ApiHeiAndMajorVersionTuple, Map<ManifestAndHostIndex, List<String>>> map
      = new HashMap<>();

  public Map<ApiHeiAndMajorVersionTuple, Map<ManifestAndHostIndex, List<String>>> getMap() {
    return map;
  }

  /**
   * Add information that `key` API, HEI, Major version uple is implemented on
   * `apiImplementationInfo`.
   * @param key
   *      API, HEI and Major version tuple for which information is added.
   * @param apiImplementationInfo
   *      describes a Manifest, Host pair where key is implemented.
   * @param version
   *      exact implemented version.
   */
  public void addEntry(ApiHeiAndMajorVersionTuple key, ManifestAndHostIndex apiImplementationInfo,
      ApiVersion version) {
    if (!this.map.containsKey(key)) {
      this.map.put(key, new HashMap<>());
    }
    Map<ManifestAndHostIndex, List<String>> hostsMap = this.map.get(key);
    if (!hostsMap.containsKey(apiImplementationInfo)) {
      hostsMap.put(apiImplementationInfo, new ArrayList<>());
    }

    hostsMap.get(apiImplementationInfo).add(version.toString());
  }

  /**
   * Creates new Mapping that contains only entries that have more than one implementation.
   * @return
   *      New Mapping with duplicates.
   */
  public ApiForHeiImplementationMapping getMappingWithDuplicates() {
    ApiForHeiImplementationMapping duplicates = new ApiForHeiImplementationMapping();
    for (Map.Entry<ApiHeiAndMajorVersionTuple, Map<ManifestAndHostIndex, List<String>>> entry :
        this.map.entrySet()) {
      if (entry.getValue().size() > 1) {
        duplicates.map.put(entry.getKey(), entry.getValue());
      }
    }
    return duplicates;
  }

  /**
   * Creates ApiForHeiImplementationMapping using data collected in `infos`.
   *
   * @param infos
   *      ManifestOverviewInfo list from which new ApiForHeiImplementationMapping will be generated.
   * @return
   *      ApiForHeiImplementationMapping created from infos.
   */
  public static ApiForHeiImplementationMapping fromManifestOverviewInfos(
      List<ManifestOverviewInfo> infos) {
    ApiForHeiImplementationMapping apiForHeiImplementationMapping
        = new ApiForHeiImplementationMapping();
    for (ManifestOverviewInfo info : infos) {
      String manifestUrl = info.url;
      int hostId = 0;
      for (HostOverviewInfo host : info.hosts) {
        hostId++;

        for (String heiId : host.coveredHeiIds) {
          for (ImplementedApiInfo implementedApiInfo : host.apisImplemented) {
            ManifestAndHostIndex manifestAndHostIndex =
                new ManifestAndHostIndex(manifestUrl, hostId);
            ApiHeiAndMajorVersionTuple key = new ApiHeiAndMajorVersionTuple(
                heiId, implementedApiInfo.name, implementedApiInfo.version);
            apiForHeiImplementationMapping.addEntry(
                key, manifestAndHostIndex, implementedApiInfo.version);
          }
        }
      }
    }
    return apiForHeiImplementationMapping;
  }
}
