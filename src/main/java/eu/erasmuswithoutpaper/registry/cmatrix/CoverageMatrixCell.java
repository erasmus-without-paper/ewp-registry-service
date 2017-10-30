package eu.erasmuswithoutpaper.registry.cmatrix;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.web.util.HtmlUtils;

class CoverageMatrixCell {

  private final List<String> classes;
  private final List<ContentLine> contentLines;
  private final List<String> tooltipParas;
  private int colSpan;

  CoverageMatrixCell(int colorClass) {
    this.classes = new ArrayList<>();
    this.contentLines = new ArrayList<>();
    this.tooltipParas = new ArrayList<>();
    this.colSpan = 1;

    this.addClass("ewpst__cell");
    this.addClass("ewpst__cell--cc" + colorClass);
  }

  void addClass(String className) {
    this.classes.add(className);
  }

  void addContentLine(ContentLine contentLine) {
    this.contentLines.add(contentLine);
  }

  void addContentLine(String content) {
    this.contentLines.add(new ContentLine(content));
  }

  void addTooltipLine(String content) {
    this.tooltipParas.add(content);
  }

  void renderHtmlCell(StringBuilder sb) {
    sb.append("<td");
    if (this.colSpan > 1) {
      sb.append(" colspan='").append(this.colSpan).append('\'');
    }
    sb.append(" class='");
    sb.append(String.join(" ",
        this.classes.stream().map(s -> HtmlUtils.htmlEscape(s)).collect(Collectors.toList())));
    sb.append('\'');
    if (this.tooltipParas.size() > 0) {
      sb.append(" title='");
      for (int i = 0; i < this.tooltipParas.size(); i++) {
        if (i > 0) {
          sb.append("\n\n");
        }
        sb.append(HtmlUtils.htmlEscape(this.tooltipParas.get(i)));
      }
      sb.append('\'');
    }
    sb.append('>');
    for (ContentLine line : this.contentLines) {
      line.renderHtmlLine(sb);
    }
    sb.append("</td>");
  }

  void setColSpan(int colSpan) {
    this.colSpan = colSpan;
  }

}
