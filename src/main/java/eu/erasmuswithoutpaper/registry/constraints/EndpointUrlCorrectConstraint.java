package eu.erasmuswithoutpaper.registry.constraints;

import static org.apache.commons.validator.routines.UrlValidator.NO_FRAGMENTS;
import static org.joox.JOOX.$;

import java.util.ArrayList;
import java.util.List;

import eu.erasmuswithoutpaper.registry.common.Severity;
import eu.erasmuswithoutpaper.registry.documentbuilder.KnownNamespace;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

import org.apache.commons.validator.routines.UrlValidator;
import org.joox.Match;
import org.w3c.dom.Document;

public class EndpointUrlCorrectConstraint implements ManifestConstraint {

  private final UrlValidator urlValidator =
      new UrlValidator(new String[] { "https" }, NO_FRAGMENTS);

  @Override
  public List<FailedConstraintNotice> filter(Document document, RegistryClient registryClient) {
    List<FailedConstraintNotice> notices = new ArrayList<>();
    Match root = $(document).namespaces(KnownNamespace.prefixMap());
    Match apis = root
        .xpath("mf6:host/r:apis-implemented/*[local-name()!='discovery' and local-name()!='echo']");
    for (Match api : apis.each()) {
      Match urlElements =
          apis.xpath("//*[local-name()='url' or local-name()='get-url' or local-name()='index-url'"
              + " or local-name()='update-url' or local-name()='stats-url']");
      for (Match urlElement : urlElements.each()) {
        String url = urlElement.get(0).getTextContent();
        if (!this.urlValidator.isValid(url)) {
          notices.add(new FailedConstraintNotice(Severity.ERROR,
              "URL \"" + url + "\" is not correct. The API will not be imported."));
          api.remove();
        }
      }
    }
    return notices;
  }
}
