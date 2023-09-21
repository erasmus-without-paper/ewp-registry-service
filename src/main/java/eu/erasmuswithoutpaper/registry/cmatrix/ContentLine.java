package eu.erasmuswithoutpaper.registry.cmatrix;

import java.util.ArrayList;
import java.util.List;

import org.springframework.web.util.HtmlUtils;

class ContentLine {

  private final List<String> classes;
  private final List<String> tooltipLine;
  private String content;
  private boolean isContentSafeHtml;

  ContentLine() {
    this.classes = new ArrayList<>();
    this.classes.add("ewpst__line");
    this.tooltipLine = new ArrayList<>();
    this.isContentSafeHtml = false;
  }

  ContentLine(String content) {
    this();
    this.setContent(content);
  }

  void addClass(String className) {
    this.classes.add(className);
  }

  void addTooltipLine(String content) {
    this.tooltipLine.add(content);
  }

  String getContent() {
    return this.content;
  }

  boolean isSafeHtml() {
    return this.isContentSafeHtml;
  }

  void renderHtmlLine(StringBuilder sb) {
    sb.append("<div class='").append(String.join(" ", this.classes)).append('\'');
    if (this.tooltipLine.size() > 0) {
      sb.append(" title='").append(String.join("\n", this.tooltipLine)).append('\'');
    }
    sb.append('>').append(getSafeContent()).append("</div>");
  }

  private String getSafeContent() {
    if (this.isContentSafeHtml) {
      return this.content;
    } else {
      return HtmlUtils.htmlEscape(this.content);
    }
  }

  void setContent(String content) {
    this.content = content;
  }

  void setContentSafeHtml(boolean isHtml) {
    this.isContentSafeHtml = isHtml;
  }
}
