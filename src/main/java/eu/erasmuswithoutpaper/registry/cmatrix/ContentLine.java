package eu.erasmuswithoutpaper.registry.cmatrix;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
    sb.append("<div class='");
    sb.append(String.join(" ",
        this.classes.stream().map(s -> HtmlUtils.htmlEscape(s)).collect(Collectors.toList())));
    sb.append('\'');
    if (this.tooltipLine.size() > 0) {
      sb.append(" title='");
      for (int i = 0; i < this.tooltipLine.size(); i++) {
        if (i > 0) {
          sb.append('\n');
        }
        sb.append(HtmlUtils.htmlEscape(this.tooltipLine.get(i)));
      }
      sb.append('\'');
    }
    sb.append('>');
    if (this.isContentSafeHtml) {
      sb.append(this.content);
    } else {
      sb.append(HtmlUtils.htmlEscape(this.content));
    }
    sb.append("</div>");
  }

  void renderTooltipLine(StringBuilder sb) {
    if (this.isContentSafeHtml) {
      sb.append(this.content);
    } else {
      sb.append(HtmlUtils.htmlEscape(this.content));
    }
  }

  void setContent(String content) {
    this.content = content;
  }

  void setContentSafeHtml(boolean isHtml) {
    this.isContentSafeHtml = isHtml;
  }
}
