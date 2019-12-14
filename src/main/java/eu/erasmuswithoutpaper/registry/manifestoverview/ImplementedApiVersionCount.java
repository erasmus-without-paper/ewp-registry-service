package eu.erasmuswithoutpaper.registry.manifestoverview;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ImplementedApiVersionCount {
  private String version;
  private Set<String> hosts = new HashSet<>();
  private Set<String> institutions = new HashSet<>();

  public ImplementedApiVersionCount(String version) {
    this.version = version;
  }

  /**
   * Accounts `coveredInstitutions` on behalf of this api version. It is assumed that those
   * institutions come from single host `hostName`.
   *
   * @param hostName
   *      String identifying host in a manifest.
   * @param coveredInstitutions
   *      Institutions covered by this host.
   */
  public void addInstitutionsCoveredByHost(String hostName, List<String> coveredInstitutions) {
    this.hosts.add(hostName);
    this.institutions.addAll(coveredInstitutions);
  }

  public int getHostsCount() {
    return this.hosts.size();
  }

  public int getUniqueInstitutionsCount() {
    return this.institutions.size();
  }

  public String getVersion() {
    return version;
  }
}
