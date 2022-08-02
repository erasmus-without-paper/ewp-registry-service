package eu.erasmuswithoutpaper.registry.constraints;

import static org.joox.JOOX.$;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import eu.erasmuswithoutpaper.registry.common.Severity;
import eu.erasmuswithoutpaper.registry.documentbuilder.KnownNamespace;
import eu.erasmuswithoutpaper.registryclient.ApiSearchConditions;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

import org.joox.Match;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class ApiUniqueConstraint implements ManifestConstraint {
  @Override
  public List<FailedConstraintNotice> filter(Document document, RegistryClient registryClient) {
    List<FailedConstraintNotice> notices = new ArrayList<>();
    Match root = $(document).namespaces(KnownNamespace.prefixMap());
    Match heis = root.xpath(
        "mf5:host/mf5:institutions-covered/r:hei | mf6:host/mf6:institutions-covered/r:hei");
    if (heis.isNotEmpty()) {
      // heis contains at most one element (see VerifySingleHost/Hei constraints)
      String heiCovered = heis.get(0).getAttribute("id");

      for (Match match : root.xpath("mf5:host/r:apis-implemented/* | mf6:host/r:apis-implemented/*")
          .each()) {
        String apiName = match.tag();
        if (Arrays.asList("echo", "discovery").contains(apiName)) {
          /*
           * Echo and Discovery API can be duplicated (see also ManifestOverviewManager).
           */
          continue;
        }
        String namespaceUri = match.namespaceURI();
        if (!namespaceUri.startsWith("https://github.com/erasmus-without-paper/")) {
          /*
           * Most probably, this API is not related to EWP. We will ignore this API.
           */
          continue;
        }

        ApiSearchConditions conditions = new ApiSearchConditions();
        conditions.setRequiredHei(heiCovered);
        conditions.setApiClassRequired(namespaceUri, apiName);
        Collection<Element> apis = registryClient.findApis(conditions);
        String url = getUrl(match, namespaceUri);
        for (Element api : apis) {
          if (!url.equals(getUrl($(api), namespaceUri))) {
            notices.add(new FailedConstraintNotice(Severity.ERROR,
                "API " + apiName + " is already in the registry under URL: "
                    + getUrl($(api), namespaceUri) + ". It will not be imported."));
            match.remove();
            break;
          }
        }
      }
    }
    return notices;
  }

  private String getUrl(Match apiMatch, String namespaceUri) {
    return apiMatch.namespace("entry", namespaceUri).xpath("entry:url | entry:get-url").text();
  }
}
