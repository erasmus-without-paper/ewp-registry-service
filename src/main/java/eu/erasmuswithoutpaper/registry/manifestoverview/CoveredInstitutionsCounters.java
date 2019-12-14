package eu.erasmuswithoutpaper.registry.manifestoverview;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CoveredInstitutionsCounters {
  private final Map<ManifestAndHostIndex, Map<String, Integer>> map = new HashMap<>();

  public Map<ManifestAndHostIndex, Map<String, Integer>> getMap() {
    return map;
  }

  private void addEntry(ManifestAndHostIndex manifestAndHostIndex, String heiId) {
    if (!this.map.containsKey(manifestAndHostIndex)) {
      this.map.put(manifestAndHostIndex, new HashMap<>());
    }
    if (!this.map.get(manifestAndHostIndex).containsKey(heiId)) {
      this.map.get(manifestAndHostIndex).put(heiId, 0);
    }
    int counter = this.map.get(manifestAndHostIndex).get(heiId);
    this.map.get(manifestAndHostIndex).put(heiId, counter + 1);
  }

  /**
   * Creates CoveredInstitutionsCounters from list of ManifestOverviewInfo.
   * @param infos list of ManifestOverviewInfo from which data will be read.
   * @return object with counted appearances of hei ids on institutions-covered lists.
   */
  public static CoveredInstitutionsCounters fromManifestOverviewInfos(
      List<ManifestOverviewInfo> infos) {
    CoveredInstitutionsCounters coveredInstitutionsCounters = new CoveredInstitutionsCounters();
    for (ManifestOverviewInfo info : infos) {
      String manifestUrl = info.url;
      int hostId = 0;
      for (HostOverviewInfo host : info.hosts) {
        hostId++;
        ManifestAndHostIndex manifestAndHostIndex = new ManifestAndHostIndex(
            manifestUrl, hostId);
        for (String heiId : host.coveredHeiIds) {
          coveredInstitutionsCounters.addEntry(
              manifestAndHostIndex,
              heiId
          );
        }
      }
    }
    return coveredInstitutionsCounters;
  }

  /**
   * Filters heiIds in this objects to only those which have count greater than 1.
   * @return
   *      CoveredInstitutionsCounters containing only those entries that have count greater than 1.
   */
  public CoveredInstitutionsCounters getOnlyDuplicates() {
    CoveredInstitutionsCounters coveredInstitutionsCounters = new CoveredInstitutionsCounters();
    for (Map.Entry<ManifestAndHostIndex, Map<String, Integer>> entry : this.map.entrySet()) {
      ManifestAndHostIndex manifestAndHostIndex = entry.getKey();
      Map<String, Integer> counts = entry.getValue();
      Map<String, Integer> duplicates = counts.entrySet().stream()
          .filter(e -> e.getValue() > 1)
          .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
      if (!duplicates.isEmpty()) {
        coveredInstitutionsCounters.map.put(manifestAndHostIndex, duplicates);
      }
    }
    return coveredInstitutionsCounters;
  }
}
