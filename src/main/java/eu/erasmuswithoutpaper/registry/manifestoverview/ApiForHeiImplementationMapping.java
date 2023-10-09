package eu.erasmuswithoutpaper.registry.manifestoverview;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Defines a mapping between API implemented for HEI to place (manifest, host, version) where it
 * is implemented.
 */
public class ApiForHeiImplementationMapping {
  private Map<ApiHeiAndMajorVersionTuple, Map<ManifestAndHostIndex, List<String>>> map
      = new HashMap<>();

  public Map<ApiHeiAndMajorVersionTuple, Map<ManifestAndHostIndex, List<String>>> getMap() {
    return Collections.unmodifiableMap(map);
  }

  /**
   * Add information that `key` API, HEI, Major version tuple is implemented on
   * `apiImplementationInfo`.
   *
   * @param key
   *     API, HEI and Major version tuple for which information is added.
   * @param apiImplementationInfo
   *     describes a Manifest, Host pair where key is implemented.
   * @param version
   *     exact implemented version.
   */
  private void addEntry(ApiHeiAndMajorVersionTuple key, ManifestAndHostIndex apiImplementationInfo,
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
   *
   * @return New Mapping with duplicates.
   */
  public ApiForHeiImplementationMapping getMappingWithDuplicates() {
    ApiForHeiImplementationMapping duplicates = new ApiForHeiImplementationMapping();
    for (Map.Entry<ApiHeiAndMajorVersionTuple, Map<ManifestAndHostIndex, List<String>>> entry :
        this.map.entrySet()) {
      if (isDuplicate(entry.getValue())) {
        duplicates.map.put(entry.getKey(), entry.getValue());
      }
    }
    return duplicates;
  }

  private boolean isDuplicate(Map<ManifestAndHostIndex, List<String>> heiImplementationInfo) {
    if (heiImplementationInfo.size() > 1) {
      return true;
    }

    if (heiImplementationInfo.size() == 1) {
      List<String> implementedVersions = heiImplementationInfo.values().iterator().next();
      return implementedVersions.size() > 1;
    }
    return false;
  }


  /**
   * Returns new Mapping without APIs on apisToExclude list.
   *
   * @param apisToExclude APIs to exclude from the Mapping.
   * @return New Mapping without some of the APIs.
   */
  public ApiForHeiImplementationMapping excludeExternalDuplicates(List<String> apisToExclude) {
    ApiForHeiImplementationMapping filtered = new ApiForHeiImplementationMapping();
    for (Map.Entry<ApiHeiAndMajorVersionTuple, Map<ManifestAndHostIndex, List<String>>> entry :
        this.map.entrySet()) {
      // If API is not on the list - add it to the result.
      if (!apisToExclude.contains(entry.getKey().getApiName())) {
        filtered.map.put(entry.getKey(), entry.getValue());
      } else {
        // API is on the list - get all hosts that contain duplicated API entries.
        Map<ManifestAndHostIndex, List<String>> hostsWithInternalDuplicates =
            selectHostsWithInternalDuplicates(entry.getValue());

        // Hosts that contain internal duplicates are still considered as duplicates.
        if (hostsWithInternalDuplicates.size() > 0) {
          filtered.map.put(entry.getKey(), hostsWithInternalDuplicates);
        }
      }
    }
    return filtered;
  }

  private Map<ManifestAndHostIndex, List<String>> selectHostsWithInternalDuplicates(
      Map<ManifestAndHostIndex, List<String>> value) {
    return value.entrySet().stream()
        .filter(e -> e.getValue().size() > 1)
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  /**
   * Creates ApiForHeiImplementationMapping using data collected in `infos`.
   *
   * @param infos ManifestOverviewInfo list from which new ApiForHeiImplementationMapping will be
   *             generated.
   * @return ApiForHeiImplementationMapping created from infos.
   */
  public static ApiForHeiImplementationMapping fromManifestOverviewInfos(
      Collection<ManifestOverviewInfo> infos) {
    ApiForHeiImplementationMapping apiForHeiImplementationMapping
        = new ApiForHeiImplementationMapping();
    for (ManifestOverviewInfo info : infos) {
      String manifestUrl = info.getUrl();
      int hostId = 0;
      for (HostOverviewInfo host : info.getHosts()) {
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
