package eu.erasmuswithoutpaper.registry.constraints;

import static org.joox.JOOX.$;

import java.util.ArrayList;
import java.util.List;

import eu.erasmuswithoutpaper.registry.common.Severity;
import eu.erasmuswithoutpaper.registry.documentbuilder.KnownNamespace;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

import org.joox.Match;
import org.w3c.dom.Document;

/**
 * This constraint forbids the Manifest from declaring Registry API implementations.
 */
public class ForbidRegistryImplementations implements ManifestConstraint {
  private final String registryRepoBaseUrl;
  private final String productionUrl;

  public ForbidRegistryImplementations(String registryRepoBaseUrl, String productionUrl) {
    this.registryRepoBaseUrl = registryRepoBaseUrl;
    this.productionUrl = productionUrl;
  }

  @Override
  public List<FailedConstraintNotice> filter(Document doc, RegistryClient registryClient) {
    List<FailedConstraintNotice> notices = new ArrayList<>(1);
    Match root = $(doc).namespaces(KnownNamespace.prefixMap());
    Match registryApiEntries = root.xpath("mf6:host/r:apis-implemented/r1:registry");
    if (registryApiEntries.isNotEmpty()) {
      registryApiEntries.remove();
      StringBuilder sb = new StringBuilder();
      sb.append("Only the <a href='" + productionUrl + "'>");
      sb.append("Registry Service</a> is allowed to implement the <a href='");
      sb.append(registryRepoBaseUrl + "/ewp-specs-api-registry");
      sb.append("'>Registry API</a>. Your Registry API entries will not be ");
      sb.append("imported, and should be removed from your manifest.");
      notices.add(new FailedConstraintNotice(Severity.WARNING, sb.toString()));
    }
    return notices;
  }
}
