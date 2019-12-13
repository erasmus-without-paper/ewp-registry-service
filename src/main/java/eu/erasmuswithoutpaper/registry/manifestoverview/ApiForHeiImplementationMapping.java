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
  private Map<ApiForHeiKey, List<ApiForHeiImplementationInfo>> map = new HashMap<>();

  public Map<ApiForHeiKey, List<ApiForHeiImplementationInfo>> getMap() {
    return map;
  }

  /**
   * Add information that `key` API/HEI pair is implemented on `apiImplementationInfo`.
   */
  public void addEntry(ApiForHeiKey key, ApiForHeiImplementationInfo apiImplementationInfo) {
    if (!this.map.containsKey(key)) {
      this.map.put(key, new ArrayList<>());
    }
    this.map.get(key).add(apiImplementationInfo);
  }

  /**
   * Returns new Mapping that contains only entries that have more than one implementation.
   */
  public ApiForHeiImplementationMapping getMappingWithDuplicates() {
    ApiForHeiImplementationMapping duplicates = new ApiForHeiImplementationMapping();
    for (Map.Entry<ApiForHeiKey, List<ApiForHeiImplementationInfo>> entry : this.map.entrySet()) {
      if (entry.getValue().size() > 1) {
        duplicates.map.put(entry.getKey(), entry.getValue());
      }
    }
    return duplicates;
  }

  /**
   * Creates ApiForHeiImplementationMapping using data collected in `infos`.
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
            ApiForHeiImplementationInfo apiForHeiImplementationInfo =
                new ApiForHeiImplementationInfo(manifestUrl, hostId, implementedApiInfo.version);
            ApiForHeiKey key = new ApiForHeiKey(heiId, implementedApiInfo.name);
            apiForHeiImplementationMapping.addEntry(key, apiForHeiImplementationInfo);
          }
        }
      }
    }
    return apiForHeiImplementationMapping;
  }
}
