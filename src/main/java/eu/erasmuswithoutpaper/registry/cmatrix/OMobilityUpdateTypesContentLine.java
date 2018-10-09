package eu.erasmuswithoutpaper.registry.cmatrix;

import static org.joox.JOOX.$;

import java.util.ArrayList;
import java.util.List;

import eu.erasmuswithoutpaper.registry.documentbuilder.KnownNamespace;

import org.joox.Match;
import org.w3c.dom.Element;

class OMobilityUpdateTypesContentLine extends ContentLine {

  OMobilityUpdateTypesContentLine(Element apiEntry) {

    Match entry = $(apiEntry).namespaces(KnownNamespace.prefixMap());
    List<Boolean> items = new ArrayList<>();
    boolean ut1 = entry.xpath("om1:supported-update-types/om1:approve-components-studied-draft-v1")
        .size() > 0;
    boolean ut2 =
        entry.xpath("om1:supported-update-types/om1:update-components-studied-v1").size() > 0;
    items.add(ut1);
    items.add(ut2);

    StringBuilder sb = new StringBuilder();
    for (Boolean item : items) {
      sb.append("<span class='ewpst__yesNo ewpst__updateitem");
      if (item) {
        sb.append(" ewpst__yesNo--yes");
      } else {
        sb.append(" ewpst__yesNo--no");
      }
      sb.append("'>");
      if (item) {
        sb.append('Y');
      } else {
        sb.append('N');
      }
      sb.append("</span>");
    }
    this.setContent(sb.toString());
    this.setContentSafeHtml(true);

    this.addTooltipLine(
        "The manifest states that the update endpoint supports " + "the following update types:");
    this.addTooltipLine("");
    this.addTooltipLine("approve-components-studied-draft-v1 (remote approval of LA drafts) - "
        + (ut1 ? "YES" : "NO"));
    this.addTooltipLine(
        "update-components-studied-v1 (remote editing of LA drafts) - " + (ut2 ? "YES" : "NO"));
  }
}
