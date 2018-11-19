package eu.erasmuswithoutpaper.registry.validators;

import org.apache.commons.lang.StringEscapeUtils;

import java.util.HashMap;

public class XMLBuilder {
  private StringBuilder sb;
  private HashMap<String, String> namespaces;
  private String root_name;
  private String schema_namespace;
  private String schema_url;

  public XMLBuilder(String _root_name) {
    sb = new StringBuilder();
    namespaces = new HashMap<>();
    namespaces.put("http://www.w3.org/XML/1998/namespace", "xml");
    namespaces.put("http://www.w3.org/2001/XMLSchema-instance", "xsi");
    root_name = _root_name;
  }

  public void AddSchemaLocation(String _namespace, String _url) {
    schema_namespace = _namespace;
    schema_url = _url;
  }

  public XMLBuilder append(String s) {
    sb.append(s);
    return this;
  }

  public XMLBuilder appendEscape(String s) {
    sb.append(
      StringEscapeUtils.escapeXml(s)
    );
    return this;
  }

  public void addNamespace(String _url, String _abbrev) {
    namespaces.put(_url, _abbrev);
  }

  public String getAbbrev(String _url) {
    if (namespaces.containsKey(_url)) {
      return namespaces.get(_url);
    }
    return null;
  }

  public String getOrAddAbbrev(String _url, String _abbrev) {
    String abbr = getAbbrev(_url);
    if (abbr != null) {
      return abbr;
    }
    addNamespace(_url, _abbrev);
    return _abbrev;
  }

  @Override
  public String toString() {
    StringBuilder sb2 = new StringBuilder();
    sb2.append("<");
    sb2.append(root_name);
    sb2.append("\n");

    for (String url : namespaces.keySet()) {
      String abbrev = namespaces.get(url);
      sb2.append("xmlns");
      if (!abbrev.isEmpty()) {
        sb2.append(":");
        sb2.append(abbrev);
      }
      sb2.append("=\"");
      sb2.append(url);
      sb2.append("\"\n");
    }
    if (schema_namespace != null && !schema_namespace.isEmpty()) {
      sb2.append("xsi:schemaLocation=\"").append("\n");
      sb2.append(schema_namespace).append("\n");
      sb2.append(schema_url).append("\n");
      sb2.append("\"").append("\n");
    }
    sb2.append(">\n");

    sb2.append(sb.toString());

    sb2.append("</");
    sb2.append(root_name);
    sb2.append(">");

    return sb2.toString();
  }
}

