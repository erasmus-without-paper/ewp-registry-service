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
import eu.erasmuswithoutpaper.registry.repository.ManifestNotFound;
import eu.erasmuswithoutpaper.registry.repository.ManifestRepository;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.joox.Match;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

@SuppressFBWarnings("URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
public class ManifestOverviewInfo {
  public String url;
  public List<HostOverviewInfo> hosts = new ArrayList<>();

  /**
   * Builds ManifestOverviewInfo.
   *
   * @param url
   *     url of manifest to describe.
   * @param manifestRepository
   *     repository from which manifest can be fetched.
   * @return ManifestOverviewInfo containing basic information about that manifest
   *     or null if it cannot be parsed.
   */
  public static ManifestOverviewInfo generateFromManifest(String url,
      ManifestRepository manifestRepository) {

    String manifest;
    try {
      manifest = manifestRepository.getManifestFiltered(url);
    } catch (ManifestNotFound manifestNotFound) {
      return null;
    }

    ManifestOverviewInfo result = new ManifestOverviewInfo();
    result.url = url;

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

    Match hosts = matcher.xpath("/mf5:manifest/mf5:host");
    for (Element host : hosts) {
      HostOverviewInfo hostOverviewInfo = new HostOverviewInfo();
      Match hostMatch = $(host).namespaces(KnownNamespace.prefixMap());
      for (Element elem : hostMatch.xpath("ewp:admin-email")) {
        hostOverviewInfo.adminEmails.add(elem.getTextContent());
      }
      for (Element elem : hostMatch.xpath("ewp:admin-notes")) {
        hostOverviewInfo.adminNotes.add(elem.getTextContent());
      }
      for (Element elem : hostMatch.xpath("r:apis-implemented/*")) {
        Match urlElementMatch = $(elem).xpath("*[" + endsWithUrlXPath + "]");
        List<String> apiUrls = urlElementMatch.map(x -> x.element().getTextContent());

        String elementVersionTag = elem.getAttribute("version");
        ApiVersion apiVersion = new ApiVersion(elementVersionTag);

        hostOverviewInfo.apisImplemented.add(
            new ImplementedApiInfo(
                elem.getLocalName(),
                apiVersion,
                apiUrls
            )
        );
      }
      for (Element elem : hostMatch.xpath("mf5:institutions-covered/r:hei")) {
        hostOverviewInfo.coveredHeiIds.add(elem.getAttribute("id"));
      }
      result.hosts.add(hostOverviewInfo);
    }
    return result;
  }
}
