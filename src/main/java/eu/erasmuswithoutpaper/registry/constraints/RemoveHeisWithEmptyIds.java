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
 * This constraint removes the covered institutions whose SCHAC ID is empty.
 *
 * <p>
 * The <code>id</code> attribute is declared as <code>xs:string</code>, so an empty value passes
 * XML Schema validation. Such entries cannot identify any institution, and importing them results
 * in empty <code>&lt;hei-id/&gt;</code> elements in the catalogue.
 * </p>
 */
public class RemoveHeisWithEmptyIds implements ManifestConstraint {

  @Override
  public List<FailedConstraintNotice> filter(Document doc, RegistryClient registryClient) {
    List<FailedConstraintNotice> notices = new ArrayList<>();
    Match root = $(doc).namespaces(KnownNamespace.prefixMap());
    for (Match hei : root.xpath("mf6:host/mf6:institutions-covered/r:hei").each()) {
      String id = hei.attr("id");
      if (id == null || id.isBlank()) {
        hei.remove();
        StringBuilder sb = new StringBuilder();
        sb.append("Institution covered by this host has an empty ");
        sb.append("<code>id</code> attribute. Such an entry cannot identify any institution, ");
        sb.append("so it will not be imported. Please provide a proper SCHAC identifier for it, ");
        sb.append("or remove it from your manifest");
        notices.add(new FailedConstraintNotice(Severity.ERROR, sb.toString()));
      }
    }
    return notices;
  }
}
