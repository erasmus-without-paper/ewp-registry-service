package eu.erasmuswithoutpaper.registry.constraints;

import static org.joox.JOOX.$;

import java.util.ArrayList;
import java.util.List;

import eu.erasmuswithoutpaper.registry.common.Severity;
import eu.erasmuswithoutpaper.registry.documentbuilder.KnownNamespace;

import org.joox.Match;
import org.w3c.dom.Document;

/**
 * This constraint forbids the Manifest from declaring Registry API implementations.
 */
public class ForbidRegistryImplementations implements ManifestConstraint {

  @Override
  public List<FailedConstraintNotice> filter(Document doc) {
    List<FailedConstraintNotice> notices = new ArrayList<>(1);
    Match root = $(doc).namespaces(KnownNamespace.prefixMap());
    Match registryApiEntries = root.xpath("r:apis-implemented/r1:registry");
    if (registryApiEntries.isNotEmpty()) {
      registryApiEntries.remove();
      StringBuilder sb = new StringBuilder();
      sb.append("Only the <a href='https://registry.erasmuswithoutpaper.eu/'>");
      sb.append("Registry Service</a> is allowed to implement the <a href='");
      sb.append("https://github.com/erasmus-without-paper/ewp-specs-api-registry");
      sb.append("'>Registry API</a>. Your Registry API entries will not be ");
      sb.append("imported, and should be removed from your manifest.");
      notices.add(new FailedConstraintNotice(Severity.WARNING, sb.toString()));
    }
    return notices;
  }
}
