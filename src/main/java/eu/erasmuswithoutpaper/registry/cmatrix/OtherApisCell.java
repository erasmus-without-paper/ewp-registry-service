package eu.erasmuswithoutpaper.registry.cmatrix;

import java.util.ArrayList;
import java.util.List;

import eu.erasmuswithoutpaper.registry.documentbuilder.KnownElement;
import eu.erasmuswithoutpaper.registryclient.ApiSearchConditions;
import eu.erasmuswithoutpaper.registryclient.HeiEntry;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

import org.springframework.web.util.HtmlUtils;

import org.w3c.dom.Element;

class OtherApisCell extends CoverageMatrixCell {

  final RegistryClient client;
  final HeiEntry hei;

  final List<Element> unmatched;

  OtherApisCell(int colorClass, RegistryClient client, HeiEntry hei) {
    super(colorClass);
    this.client = client;
    this.hei = hei;

    ApiSearchConditions conds = new ApiSearchConditions();
    conds.setRequiredHei(this.hei.getId());
    List<KnownElement> knownApis = KnownElement.values();
    this.unmatched = new ArrayList<>();
    for (Element apiEntry : this.client.findApis(conds)) {
      boolean matched = false;
      for (KnownElement apiDef : knownApis) {
        if (apiDef.matches(apiEntry)) {
          matched = true;
          break;
        }
      }
      if (!matched) {
        this.unmatched.add(apiEntry);
      }
    }

    if (!this.unmatched.isEmpty()) {
      StringBuilder sb = new StringBuilder();
      sb.append("<a href='#").append(HtmlUtils.htmlEscape(this.getFootnoteId())).append("'>");
      sb.append('+').append(this.unmatched.size()).append(" unknown APIs");
      sb.append("</a>");
      ContentLine line = new ContentLine();
      line.setContent(sb.toString());
      line.setContentSafeHtml(true);
      for (Element apiEntry : this.unmatched) {
        line.addTooltipLine(
            "{" + HtmlUtils.htmlEscape(apiEntry.getNamespaceURI()) + "}" + apiEntry.getLocalName());
      }
      this.addContentLine(line);
    }
  }

  private String getFootnoteId() {
    return "footnote-" + this.hei.getId();
  }

  void renderHtmlFootnote(StringBuilder sb) {
    if (this.unmatched.size() == 0) {
      return;
    }
    sb.append("<p id='").append(HtmlUtils.htmlEscape(this.getFootnoteId())).append("'>");
    sb.append("<code class='ewpst__bordered-code'>");
    sb.append(HtmlUtils.htmlEscape(this.hei.getId())).append("</code>");
    sb.append(" implements the following unknown APIs:</p>");
    sb.append("<ul>");
    for (Element apiEntry : this.unmatched) {
      sb.append("<li><code>");
      sb.append('{').append(HtmlUtils.htmlEscape(apiEntry.getNamespaceURI())).append('}');
      sb.append(apiEntry.getLocalName());
      sb.append("</code></li>");
    }
    sb.append("</ul>");
  }
}
