package eu.erasmuswithoutpaper.registry.constraints;

import static org.joox.JOOX.$;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.List;

import eu.erasmuswithoutpaper.registry.common.Severity;
import eu.erasmuswithoutpaper.registry.documentbuilder.KnownNamespace;
import eu.erasmuswithoutpaper.registryclient.ApiSearchConditions;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

import org.joox.Match;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class EndpointUniqueConstraint implements ManifestConstraint {

  private final KeyFactory rsaFactory;

  /**
   * Initialize dependencies.
   */
  public EndpointUniqueConstraint() {
    try {
      rsaFactory = KeyFactory.getInstance("RSA");
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public List<FailedConstraintNotice> filter(Document document, RegistryClient registryClient) {
    List<FailedConstraintNotice> notices = new ArrayList<>();
    Match root = $(document).namespaces(KnownNamespace.prefixMap());
    Match serverKeys = root.xpath(
        "mf5:host/mf5:server-credentials-in-use/mf5:rsa-public-key | "
            + "mf6:host/mf6:server-credentials-in-use/mf6:rsa-public-key");
    if (serverKeys.isNotEmpty()) {
      Collection<RSAPublicKey> rsaPublicKeys = getRsaPublicKeys(serverKeys);

      for (Match match : root.xpath("mf5:host/r:apis-implemented/* | mf6:host/r:apis-implemented/*")
          .each()) {
        String apiName = match.tag();
        String namespaceUri = match.namespaceURI();
        if (!namespaceUri.startsWith("https://github.com/erasmus-without-paper/")) {
          /*
           * Most probably, this API is not related to EWP. We will ignore this API.
           */
          continue;
        }

        ApiSearchConditions conditions = new ApiSearchConditions();
        conditions.setApiClassRequired(namespaceUri, apiName);
        Collection<Element> apis = registryClient.findApis(conditions);
        String url = getUrl(match, namespaceUri);
        for (Element api : apis) {
          if (url.equals(getUrl($(api), namespaceUri))) {
            Collection<RSAPublicKey> serverKeysCoveringApi =
                registryClient.getServerKeysCoveringApi(api);

            if (!rsaPublicKeys.containsAll(serverKeysCoveringApi)) {
              notices.add(new FailedConstraintNotice(Severity.ERROR,
                  "API " + apiName + " is already in the registry under the same URL: "
                      + getUrl($(api), namespaceUri) + ". It will not be imported."));
              match.remove();
              break;
            }
          }
        }
      }
    }
    return notices;
  }

  private Collection<RSAPublicKey> getRsaPublicKeys(Match serverKeys) {
    List<RSAPublicKey> rsaPublicKeys = new ArrayList<>(serverKeys.size());
    for (Match serverKey : serverKeys.each()) {
      String keyStr = serverKey.text().replaceAll("\\s+", "");
      byte[] decoded = Base64.getDecoder().decode(keyStr);
      try {
        rsaPublicKeys
            .add((RSAPublicKey) rsaFactory.generatePublic(new X509EncodedKeySpec(decoded)));
      } catch (InvalidKeySpecException e) {
        throw new RuntimeException(e); // Should not be possible after ServerKeySecurityConstraint
      }
    }
    return rsaPublicKeys;
  }

  private String getUrl(Match apiMatch, String namespaceUri) {
    return apiMatch.namespace("entry", namespaceUri).xpath("entry:url | entry:get-url").text();
  }
}
