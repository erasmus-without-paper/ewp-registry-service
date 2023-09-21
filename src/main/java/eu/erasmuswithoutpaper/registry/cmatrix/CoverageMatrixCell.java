package eu.erasmuswithoutpaper.registry.cmatrix;

import java.util.ArrayList;
import java.util.List;

class CoverageMatrixCell {

  private final List<String> classes;
  private final List<ContentLine> contentLines;
  private int colSpan;

  CoverageMatrixCell(int colorClass) {
    this.classes = new ArrayList<>();
    this.contentLines = new ArrayList<>();
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

  void renderHtmlCell(StringBuilder sb) {
    sb.append("<td");
    if (this.colSpan > 1) {
      sb.append(" colspan='").append(this.colSpan).append('\'');
    }
    sb.append(" class='").append(String.join(" ", this.classes)).append("'>");
    for (ContentLine line : this.contentLines) {
      line.renderHtmlLine(sb);
    }
    sb.append("</td>");
  }

  void setColSpan(int colSpan) {
    this.colSpan = colSpan;
  }

}
