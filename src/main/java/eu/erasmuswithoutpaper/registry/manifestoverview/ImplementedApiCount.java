package eu.erasmuswithoutpaper.registry.manifestoverview;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ImplementedApiCount {
  private String name;
  private Map<String, ImplementedApiVersionCount> countsMap;

  /**
   * Counts API implementations.
   * @param name name of api.
   */
  public ImplementedApiCount(String name) {
    this.name = name;
    this.countsMap = new HashMap<>();
  }

  /**
   * Accounts that this api in `version` version is used by `institutions`.
   * @param hostName
   *      String representing a host in a manifest.
   * @param version
   *      String representing implemented version of API stored in this object.
   * @param institutions
   *      Institutions covered by this host.
   */
  public void add(String hostName, String version, List<String> institutions) {
    if (!countsMap.containsKey(version)) {
      countsMap.put(version, new ImplementedApiVersionCount(version));
    }
    countsMap.get(version).addInstitutionsCoveredByHost(hostName, institutions);

    final String allVersions = "all";
    if (!countsMap.containsKey(allVersions)) {
      countsMap.put(allVersions, new ImplementedApiVersionCount(allVersions));
    }
    countsMap.get(allVersions).addInstitutionsCoveredByHost(hostName, institutions);
  }

  public String getName() {
    return name;
  }

  /**
   * Returns collected statistics, sorted by version, descending.
   * @return
   *      List of ImplementedApiVersionCount.
   */
  public List<ImplementedApiVersionCount> getCounts() {
    List<ImplementedApiVersionCount> countsList = new ArrayList<>(this.countsMap.values());
    countsList.sort(Comparator.comparing(ImplementedApiVersionCount::getVersion).reversed());
    return countsList;
  }
}
