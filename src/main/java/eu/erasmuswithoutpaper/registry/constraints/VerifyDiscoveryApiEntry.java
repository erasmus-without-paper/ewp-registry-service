package eu.erasmuswithoutpaper.registry.constraints;

import static org.joox.JOOX.$;

import java.util.ArrayList;
import java.util.List;

import eu.erasmuswithoutpaper.registry.common.Severity;
import eu.erasmuswithoutpaper.registry.common.Utils;
import eu.erasmuswithoutpaper.registry.documentbuilder.KnownNamespace;

import org.joox.Match;
import org.w3c.dom.Document;

/**
 * This constraint helps to verify if the manifest properly describes itself (if its own Discovery
 * API entry includes the URL which the manifest has been fetched from).
 */
public class VerifyDiscoveryApiEntry implements ManifestConstraint {

  private final String expectedUrl;

  /**
   * @param url The URL which the manifest has been fetched from. It if isn't found among the
   *        Discovery API entries, the author will be notified.
   */
  public VerifyDiscoveryApiEntry(String url) {
    this.expectedUrl = url;
  }

  @Override
  public List<FailedConstraintNotice> filter(Document doc) {
    List<FailedConstraintNotice> notices = new ArrayList<>(1);
    Match root = $(doc).namespaces(KnownNamespace.prefixMap());
    if (!root
        .xpath("mf5:host/r:apis-implemented/d5:discovery/d5:url | "
            + "mf6:host/r:apis-implemented/d6:discovery/d6:url")
        .texts().contains(this.expectedUrl)) {
      StringBuilder sb = new StringBuilder();
      sb.append("We have found an inconsistency in your Discovery API manifest. We were ");
      sb.append("expecting to find this URL in one of your discovery/url elements, but we ");
      sb.append("didn't:<ul><li><code>");
      sb.append(Utils.escapeHtml(this.expectedUrl) + "</code></li></ul>");
      sb.append("<p>This is not vital for most EWP clients, because Discovery API is usually ");
      sb.append("accessed by the Registry Service only, but still, it seems to be a small bug ");
      sb.append("that you should fix.</p>");
      notices.add(new FailedConstraintNotice(Severity.WARNING, sb.toString()));
    }
    return notices;
  }
}
