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
 * This constraint allows the manifest to contain only a single host.
 */
public class VerifySingleHost implements ManifestConstraint {

  @Override
  public List<FailedConstraintNotice> filter(Document doc, RegistryClient registryClient) {
    List<FailedConstraintNotice> notices = new ArrayList<>();
    Match root = $(doc).namespaces(KnownNamespace.prefixMap());
    if (root.xpath("mf5:host").size() > 1) {
      notices.add(new FailedConstraintNotice(Severity.ERROR,
          "Manifest file must not contain more than one host element. "
              + "Additional elements will be removed."));
      root.xpath("mf5:host[position()>1]").remove();
    }
    return notices;
  }
}
