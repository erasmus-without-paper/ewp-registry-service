package eu.erasmuswithoutpaper.registry.constraints;

import static org.joox.JOOX.$;

import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import eu.erasmuswithoutpaper.registry.common.Severity;
import eu.erasmuswithoutpaper.registry.common.Utils;
import eu.erasmuswithoutpaper.registry.documentbuilder.KnownNamespace;
import eu.erasmuswithoutpaper.registryclient.ApiSearchConditions;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

import org.joox.Match;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class EndpointUniqueConstraint implements ManifestConstraint {

  private final String registryRepoBaseUrl;

  /**
   * Initialize dependencies.
   * @param registryRepoBaseUrl URL of the registries GitHub repository.
   */
  public EndpointUniqueConstraint(String registryRepoBaseUrl) {
    this.registryRepoBaseUrl = registryRepoBaseUrl;
  }

  @Override
  public List<FailedConstraintNotice> filter(Document document, RegistryClient registryClient) {
    List<FailedConstraintNotice> notices = new ArrayList<>();
    Match root = $(document).namespaces(KnownNamespace.prefixMap());
    Match heis = root.xpath("mf6:host/mf6:institutions-covered/r:hei");
    if (heis.isNotEmpty()) {
      // heis contains at most one element (see VerifySingleHost/Hei constraints)
      String heiCovered = heis.get(0).getAttribute("id");

      for (Match match : root.xpath("mf6:host/r:apis-implemented/*")
          .each()) {
        String apiName = match.tag();
        if ("echo".equals(apiName) || "discovery".equals(apiName)) {
          // We don't handle Echo and Discovery APIs, as thisApis wouldn't need to be *this* API.
          continue;
        }
        String namespaceUri = match.namespaceURI();
        if (!namespaceUri.startsWith(registryRepoBaseUrl)) {
          /*
           * Most probably, this API is not related to EWP. We will ignore this API.
           */
          continue;
        }

        ApiSearchConditions conditions = new ApiSearchConditions();
        conditions.setApiClassRequired(namespaceUri, apiName);

        Collection<Element> allApis = registryClient.findApis(conditions);
        conditions.setRequiredHei(heiCovered);
        Optional<Element> thisApis = registryClient.findApis(conditions).stream().findFirst();

        Collection<RSAPublicKey> serverKeys = null;
        if (thisApis.isPresent()) {
          serverKeys = registryClient.getServerKeysCoveringApi(thisApis.get());
        }

        String url = getUrl(match.get(0));
        for (Element api : allApis) {
          if (url != null && url.equals(getUrl(api))) {
            Collection<RSAPublicKey> serverKeysCoveringApi =
                registryClient.getServerKeysCoveringApi(api);

            // If this API is not in the catalogue, or we found an API with other server keys
            if (serverKeys == null || !serverKeys.containsAll(serverKeysCoveringApi)) {
              notices.add(new FailedConstraintNotice(Severity.ERROR,
                  "API " + apiName + " is already in the registry under the same URL: "
                      + getUrl(api) + ". It will not be imported."));
              match.remove();
              break;
            }
          }
        }
      }
    }
    return notices;
  }

  private String getUrl(Element element) {
    synchronized (element.getOwnerDocument()) {
      for (Node child : Utils.asNodeList(element.getChildNodes())) {
        if (Arrays.asList("url", "get-url").contains(child.getLocalName())) {
          return child.getTextContent();
        }
      }
      return null;
    }
  }
}
