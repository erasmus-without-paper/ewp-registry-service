package eu.erasmuswithoutpaper.registry.manifestoverview;

import static org.joox.JOOX.$;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;

import eu.erasmuswithoutpaper.registry.common.Utils;
import eu.erasmuswithoutpaper.registry.documentbuilder.KnownNamespace;

import org.joox.Match;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

public class ManifestOverviewInfo {

  private final String url;
  private final List<HostOverviewInfo> hosts = new ArrayList<>();

  /**
   * Builds ManifestOverviewInfo.
   *
   * @param url
   *     url of manifest to describe.
   * @param manifest
   *     contents of manifest to describe.
   * @return ManifestOverviewInfo containing basic information about that manifest
   *     or null if it cannot be parsed.
   */
  public static ManifestOverviewInfo generateFromManifest(String url, String manifest) {
    DocumentBuilder docBuilder = Utils.newSecureDocumentBuilder();
    Document doc;
    try {
      byte[] manifestBytes = manifest.getBytes(StandardCharsets.UTF_8);
      doc = docBuilder.parse(new ByteArrayInputStream(manifestBytes));
    } catch (SAXException | IOException e) {
      return null;
    }

    Match matcher = $(doc.getDocumentElement()).namespaces(KnownNamespace.prefixMap());
    final String endsWithUrlXPath =
        "substring(local-name(), string-length(local-name()) - string-length('url') + 1) = 'url'";

    ManifestOverviewInfo result = new ManifestOverviewInfo(url);

    Match hosts = matcher.xpath("/mf6:manifest/mf6:host");
    for (Element host : hosts) {
      HostOverviewInfo hostOverviewInfo = new HostOverviewInfo();
      Match hostMatch = $(host).namespaces(KnownNamespace.prefixMap());
      for (Element elem : hostMatch.xpath("ewp:admin-email")) {
        hostOverviewInfo.adminEmails.add(elem.getTextContent());
      }
      hostOverviewInfo.adminProvider = hostMatch.xpath("ewp:admin-provider").text();
      for (Element elem : hostMatch.xpath("ewp:admin-notes")) {
        hostOverviewInfo.adminNotes.add(elem.getTextContent());
      }
      for (Element elem : hostMatch.xpath("r:apis-implemented/*")) {
        Match urlElementMatch = $(elem).xpath("*[" + endsWithUrlXPath + "]");
        List<String> apiUrls = urlElementMatch.map(x -> x.element().getTextContent());

        String elementVersionTag = elem.getAttribute("version");
        ApiVersion apiVersion = new ApiVersion(elementVersionTag);

        hostOverviewInfo.apisImplemented
            .add(new ImplementedApiInfo(elem.getLocalName(), apiVersion, apiUrls));
      }
      for (Element elem : hostMatch.xpath("mf6:institutions-covered/r:hei")) {
        hostOverviewInfo.coveredHeiIds.add(elem.getAttribute("id"));
      }
      result.hosts.add(hostOverviewInfo);
    }
    return result;
  }

  public List<HostOverviewInfo> getHosts() {
    return hosts;
  }

  public String getUrl() {
    return url;
  }

  private ManifestOverviewInfo(String url) {
    this.url = url;
  }

}
