package eu.erasmuswithoutpaper.registry.manifestoverview;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ImplementedApisCount {
  private Map<String, ImplementedApiCount> countsMap = new HashMap<>();

  /**
   * Counts institutions and hosts that use certain APIs in certain versions.
   *
   * @param overviewInfos
   *          List of ManifestOverviewInfos from which institutions will be taken.
   * @return ImplementedApisCount build from List of ManifestOverviewInfos.
   */
  public static ImplementedApisCount fromManifestOverviewInfos(
      Collection<ManifestOverviewInfo> overviewInfos) {
    ImplementedApisCount result = new ImplementedApisCount();
    for (ManifestOverviewInfo manifest : overviewInfos) {
      int hostId = 1;

      for (HostOverviewInfo host : manifest.getHosts()) {
        String hostName = manifest.getUrl() + hostId;
        hostId++;

        for (ImplementedApiInfo api : host.apisImplemented) {
          if (!result.countsMap.containsKey(api.getName())) {
            result.countsMap.put(api.getName(), new ImplementedApiCount(api.getName()));
          }
          result.countsMap.get(api.getName())
              .add(hostName, api.getVersion().toString(), host.coveredHeiIds);
        }
      }
    }
    return result;
  }

  /**
   * Returns collected statistics, sorted by api name, descending.
   *
   * @return List of ImplementedApiCount stored in this object.
   */
  public List<ImplementedApiCount> getCounts() {
    List<ImplementedApiCount> countsList = new ArrayList<>(this.countsMap.values());
    countsList.sort(Comparator.comparing(ImplementedApiCount::getName));
    return countsList;
  }
}
