package eu.erasmuswithoutpaper.registry.constraints;

import static org.joox.JOOX.$;

import java.util.ArrayList;
import java.util.List;

import eu.erasmuswithoutpaper.registry.common.Severity;
import eu.erasmuswithoutpaper.registry.documentbuilder.KnownNamespace;

import org.joox.Match;
import org.w3c.dom.Document;

/**
 * This constraint allows the manifest to contain only a single host.
 */
public class VerifySingleHost implements ManifestConstraint {

  @Override
  public List<FailedConstraintNotice> filter(Document doc) {
    List<FailedConstraintNotice> notices = new ArrayList<>();
    Match root = $(doc).namespaces(KnownNamespace.prefixMap());
    if (root.xpath("mf5:host").size() > 1) {
      notices.add(new FailedConstraintNotice(Severity.WARNING,
          "Manifest file must not contain more than one host element."));
    }
    return notices;
  }
}
