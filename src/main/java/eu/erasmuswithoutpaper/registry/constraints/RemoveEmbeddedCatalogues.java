package eu.erasmuswithoutpaper.registry.constraints;

import static org.joox.JOOX.$;

import java.util.ArrayList;
import java.util.List;

import eu.erasmuswithoutpaper.registry.common.Severity;
import eu.erasmuswithoutpaper.registry.documentbuilder.KnownNamespace;

import org.joox.Match;
import org.w3c.dom.Document;

/**
 * This constraint prevents the manifests from attempting to exploit invalid Registry clients by
 * including embedded, non-validated &lt;catalogue&gt; elements inside the &lt;apis-implemented&gt;
 * element.
 */
public class RemoveEmbeddedCatalogues implements ManifestConstraint {

  @Override
  public List<FailedConstraintNotice> filter(Document doc) {

    List<FailedConstraintNotice> notices = new ArrayList<>(1);
    Match root = $(doc).namespaces(KnownNamespace.prefixMap());

    // Search for elements from catalogue and manifest namespaces in suspicious places.

    List<Match> elems = root.xpath("r:apis-implemented//r:*").each();
    elems.addAll(root.xpath("r:apis-implemented//mf:*").each());

    // Remove them.

    for (Match match : elems) {
      match.remove();
    }

    if (!elems.isEmpty()) {
      StringBuilder sb = new StringBuilder();
      sb.append("<p>For security reasons, descendants of the &ltapis-implemented&gt; element ");
      sb.append("are not allowed to reside in Registry API and Discovery API namespaces. ");
      sb.append("These elements will not be imported, and you should remove them from your ");
      sb.append("manifest.</p>");
      notices.add(new FailedConstraintNotice(Severity.WARNING, sb.toString()));
    }

    return notices;
  }

}
