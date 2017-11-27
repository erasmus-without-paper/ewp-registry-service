package eu.erasmuswithoutpaper.registry.constraints;

import static org.joox.JOOX.$;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import eu.erasmuswithoutpaper.registry.common.Severity;
import eu.erasmuswithoutpaper.registry.common.Utils;
import eu.erasmuswithoutpaper.registry.documentbuilder.KnownNamespace;

import org.joox.Match;
import org.w3c.dom.Document;

/**
 * This constraint allows the manifest to cover only certain institutions.
 */
public class RestrictInstitutionsCovered implements ManifestConstraint {

  private final Pattern allowedSchacIdRegex;

  /**
   * @param allowedSchacIdRegex A regular expression which will be applied to all HEI IDs which the
   *        manifest attempts to cover. If HEI ID doesn't match this expression, then HEI will be
   *        removed from the list of covered institutions.
   */
  public RestrictInstitutionsCovered(String allowedSchacIdRegex) {
    this.allowedSchacIdRegex = Pattern.compile(allowedSchacIdRegex);
  }

  @Override
  public List<FailedConstraintNotice> filter(Document doc) {
    List<FailedConstraintNotice> notices = new ArrayList<>();
    Match root = $(doc).namespaces(KnownNamespace.prefixMap());
    for (Match hei : root.xpath("mf5:host/mf5:institutions-covered/r:hei").each()) {
      String id = hei.attr("id");
      if (!this.allowedSchacIdRegex.matcher(id).matches()) {
        hei.remove();
        StringBuilder sb = new StringBuilder();
        sb.append("Institution <code>" + Utils.escapeHtml(id) + "</code> didn't match the ");
        sb.append("<code>" + Utils.escapeHtml(this.allowedSchacIdRegex.pattern()) + "</code>");
        sb.append(" filter pattern which is currently assigned to this manifest source. ");
        sb.append("This HEI will not be imported. Please contact Registry Service maintainers.");
        notices.add(new FailedConstraintNotice(Severity.ERROR, sb.toString()));
      }
    }
    return notices;
  }
}
